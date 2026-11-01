package mtrultra.mixin;

/**
 * [U4 伴生] 步长放大中转变量的共享容器。
 * <p>
 * <b>为什么需要独立的 holder 类</b>：
 * sponge-mixin 0.8.7 的 {@code MixinPreProcessorStandard.validateField} 强制
 * mixin 类里的静态字段必须是 {@code private}，否则抛
 * {@code InvalidMixinException: contains non-private static field}。
 * <p>
 * 但步长值需要 {@link TrainServerMixin}（写入）与
 * {@link TrainServerTickScaleMixin}（读取）两个 mixin 共享，
 * 而 mixin 里的 private 字段无法跨类访问。
 * <p>
 * 解法：把变量放进一个**普通（非 mixin）类** {@link UltraInterval}，
 * 两个 mixin 都引用它 —— 不受 mixin 字段可见性规则约束。
 * <p>
 * [历史] 2026-10-21 起 ULTRA_INTERVAL 直接声明在 TrainServerTickScaleMixin 里
 * （包级可见 static final）。该写法在 sponge-mixin 0.8.7 下会抛
 * InvalidMixinException，且该异常只在 <b>TrainServer 类首次加载</b> 时才暴露
 * —— 也就是<b>创建车厂</b>（{@code Siding.setSidingData} → {@code TrainServer}
 * 类加载）时才崩，启动阶段反而不报错，极难定位。
 */
final class UltraInterval {

	private static final ThreadLocal<Integer> VALUE = ThreadLocal.withInitial(() -> 1);

	private UltraInterval() {
	}

	static int get() {
		return VALUE.get();
	}

	static void set(int interval) {
		VALUE.set(interval);
	}
}
