package com.example.autolibrarian;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(AutoLibrarian.MODID)
public class AutoLibrarian
{
    // Mod IDの定義
    public static final String MODID = "autolibrarian";
    private static final Logger LOGGER = LogUtils.getLogger();

    // チャットやログ用の接頭辞
    public static String ModPreFix = "§d[AutoLibrarian] ";

    // 【このModの構成…】
    // AutoLibrarian.java = このModの基礎クラス。これがないと全てのクラスが動かない。
    // KeyHandler.java = キー設定や、キーボード操作を受け取るクラス。主にMod全体の動作を取り仕切る「IsEnabled」変数を管理している。
    // AutoRerollHandler.java = メイン処理クラス。Tickイベントハンドラがあり、これで主な処理を行っている。
    // ConfigScreen.java = 設定画面を作っているクラス。呼び出しはKeyHandler.javaから。
    // EnchantmentListWidget.java = 設定画面のうち、エンチャント本のリストデータ部分を構成しているクラス。ConfigScreenで使っている。リスト表示そのものはConfigScreenが作っている。
    // ModConfig.java = Mod設定内容を管理するクラス。AutoRerollHandlerやConfigScreenはここから情報を得たり、書き換えたりしている。
    // src/main/resources/assets/autolibrarian/lang 内のJson = 翻訳ファイル。各クラスに記述されているキーと対応している。Minecraftの言語を変えると該当のJsonが読み込まれてModの表記が変わる。

    public AutoLibrarian(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // 共通セットアップイベントの登録
        modEventBus.addListener(this::commonSetup);

        // ゲーム内イベント（キー入力やTickイベントなど）を登録するために必須！
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("AutoLibrarian initialized!");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // サーバー起動時の処理（必要であれば）
    }

    // クライアント側（画面・キー操作等）のイベントリスナー
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // 設定データ読み込み
            ModConfig.load();
            LOGGER.info("AutoLibrarian Client Setup Complete. User: {}", Minecraft.getInstance().getUser().getName());
        }
    }
}