package net.systemfileb.neomafishmod.mafish_crafting;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModList;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.IEventBus;

import net.minecraft.util.Tuple;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Collection;
import java.util.ArrayList;

@Mod("mafish_crafting")
public class MafishCraftingMod {
	public static final Logger LOGGER = LogManager.getLogger(MafishCraftingMod.class);
	public static final String MODID = "mafish_crafting";

	public MafishCraftingMod(IEventBus modEventBus) {
		// Start of user code block mod constructor
		// End of user code block mod constructor
		NeoForge.EVENT_BUS.register(this);
		modEventBus.addListener(this::registerNetworking);
		// Start of user code block mod init
		if (!ModList.get().isLoaded("mafish_extras_jij")) {
			LOGGER.info("\r\n" + //
					".__  __          __  _       _        _____               __  _    _\r\n" + // 为什么这里加了一个点呢，因为第一个字符 (好像) 不能用空格 ()
					"|  \\/  |        / _|(_)     | |      / ____|             / _|| |  (_)\r\n" + //
					"| \\  / |  __ _ | |_  _  ___ | |__   | |      _ __  __ _ | |_ | |_  _  _ __    __ _\r\n" + //
					"| |\\/| | / _` ||  _|| |/ __|| '_ \\  | |     | '__|/ _` ||  _|| __|| || '_ \\  / _` |\r\n" + //
					"| |  | || (_| || |  | |\\__ \\| | | | | |____ | |  | (_| || |  | |_ | || | | || (_| |\r\n" + //
					"|_|  |_| \\__,_||_|  |_||___/|_| |_|  \\_____||_|   \\__,_||_|   \\__||_||_| |_| \\__, |\r\n" + //
					"                                                                              __/ |\r\n" + //
					"                                                                             |___/\r\n" + //
					"已被加载!   Is Loaded!\r\n" + //
					"玩得开心!   Have Fun!  ;)\r\n" + //
					"\r\n" + //
					"");
		}
		// End of user code block mod init
	}

	// Start of user code block mod methods
	// End of user code block mod methods
	private static boolean networkingRegistered = false;
	private static final Map<CustomPacketPayload.Type<?>, NetworkMessage<?>> MESSAGES = new HashMap<>();

	private record NetworkMessage<T extends CustomPacketPayload>(StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
	}

	public static <T extends CustomPacketPayload> void addNetworkMessage(CustomPacketPayload.Type<T> id, StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
		if (networkingRegistered)
			throw new IllegalStateException("Cannot register new network messages after networking has been registered");
		MESSAGES.put(id, new NetworkMessage<>(reader, handler));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void registerNetworking(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar(MODID);
		MESSAGES.forEach((id, networkMessage) -> registrar.playBidirectional(id, ((NetworkMessage) networkMessage).reader(), ((NetworkMessage) networkMessage).handler()));
		networkingRegistered = true;
	}

	private static final Collection<Tuple<Runnable, Integer>> workQueue = new ConcurrentLinkedQueue<>();

	public static void queueServerWork(int tick, Runnable action) {
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER)
			workQueue.add(new Tuple<>(action, tick));
	}

	@SubscribeEvent
	public void tick(ServerTickEvent.Post event) {
		List<Tuple<Runnable, Integer>> actions = new ArrayList<>();
		workQueue.forEach(work -> {
			work.setB(work.getB() - 1);
			if (work.getB() == 0)
				actions.add(work);
		});
		actions.forEach(e -> e.getA().run());
		workQueue.removeAll(actions);
	}
}
