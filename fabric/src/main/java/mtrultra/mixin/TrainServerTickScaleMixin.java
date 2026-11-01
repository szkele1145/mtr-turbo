package mtrultra.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * [U4 伴生] 把 TrainServer.simulateTrain 内传给父类的 ticksElapsed 放大为 LOD 间隔。
 * <p>
 * 字节码核实：TrainServer.simulateTrain(8参) 体内唯一的父类调用是
 * Train.simulateTrain(Level, float, Depot)——ticksElapsed 作为第 2 参（index=1）传入。
 * 全部模拟推进（速度积分/停靠计时/门动画）都发生在这条父类调用内，
 * 在该调用点把 float 参数乘以 interval 即完成"单次调用多 tick 积分"。
 * <p>
 * @ModifyArg 只改这一个调用点的参数——方法体内其他对 ticksElapsed 的
 * 读取不受影响（排程 arrivalMillis 用的是 currentMillis，与 ticksElapsed 无关）。
 * <p>
 * 间隔值经静态 ThreadLocal 中转（TrainServerMixin HEAD 注入写入；
 * 服务线程串行执行 simulateTrain，无并发问题）。
 */
@Mixin(targets = "mtr.data.TrainServer", remap = false)
public abstract class TrainServerTickScaleMixin {

	/**
	 * [修复 2026-11-01] 变量已移入独立的 {@link UltraInterval} holder 类。
	 * <p>
	 * 原写法是本 mixin 内的包级可见 {@code static final ThreadLocal ULTRA_INTERVAL}，
	 * 但 sponge-mixin 0.8.7 的 {@code MixinPreProcessorStandard.validateField}
	 * 强制 mixin 类里的静态字段必须是 private，否则抛
	 * {@code InvalidMixinException: contains non-private static field}。
	 * 该异常只在 <b>TrainServer 类首次加载</b>（= 创建车厂）时才暴露，
	 * 启动阶段不报错，极难定位。
	 */

	/**
	 * 放大父类调用的 ticksElapsed（index=1，0-based）。
	 * 目标调用在 8 参 simulateTrain 内唯一。
	 */
	@ModifyArg(
			method = "simulateTrain",
			at = @At(value = "INVOKE", target = "Lmtr/data/Train;simulateTrain(Lnet/minecraft/class_1937;FLmtr/data/Depot;)V"),
			index = 1,
			// [修复 2026-10-22] 补 require=0：MTR 升级改父类调用签名时
			// 注入失败仅告警不崩（步长放大失效 = LOD 退化为每 tick 模拟，行为退化但不崩）。
			require = 0
	)
	private float mtrultra$scaleTicksElapsed(float original) {
		final int interval = UltraInterval.get();
		return interval > 1 ? original * interval : original;
	}
}
