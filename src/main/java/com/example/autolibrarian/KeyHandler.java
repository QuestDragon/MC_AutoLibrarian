package com.example.autolibrarian;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public class KeyHandler {

    // Finalって？：いわゆるConstのようなもの。1度だけしか代入できない変数。今回の下の例ではもう代入しちゃってるのでこの先値の変更は出来ない。

    // Nキーの登録データ
    // KeyMappingの内容は：設定名称ID、入力の種類、デフォルト値、設定カテゴリID の順。
    // ID2つはLangフォルダ内の翻訳ファイルで使用する。
    public static final KeyMapping TOGGLE_KEY = new KeyMapping(
            "key.autolibrarian.toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "category.autolibrarian"
    );

    public static final KeyMapping CONFIG_KEY = new KeyMapping(
            "key.autolibrarian.menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "category.autolibrarian"
    );

    private static boolean Mod_isEnabled = false;
    public static boolean Get_isEnabled(){
        return Mod_isEnabled;
    }
    public static boolean Set_isEnabled(Minecraft mc, boolean Enable, boolean ShowMessage, boolean PlaySound){
        Component msg;
        Mod_isEnabled = Enable;

        if (PlaySound) mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.0f); // SE
        if (Enable) msg = Component.translatable("general.autolibrarian.on");
        else msg = Component.translatable("general.autolibrarian.off");

        if (ShowMessage) mc.player.sendSystemMessage(Component.literal(AutoLibrarian.ModPreFix + msg.getString()));
        return Enable;
    }


    // ① キーを操作設定に登録するクラス（MODバス用） -> 起動時に行われる処理はMODバスを使う。
    @Mod.EventBusSubscriber(modid = "autolibrarian", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerKeyBinding(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_KEY);
            event.register(CONFIG_KEY);
        }

    }

    // ② 実際にキーが押されたのを検知するクラス（FORGEバス用） -> プレイ中に行われるキー操作はForgeバスを使う。
    @Mod.EventBusSubscriber(modid = "autolibrarian", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientForgeEvents {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            while (TOGGLE_KEY.consumeClick()) {
                // マイクラゲーム本体を取得
                Minecraft mc = Minecraft.getInstance();

                Set_isEnabled(mc, !Mod_isEnabled, Mod_isEnabled, Mod_isEnabled); // 反転、OFF時のみ通知ON

                Player player = mc.player;
                // Nullチェック（ぬるぽ回避）
                if (player != null) {
                    if (Mod_isEnabled) {
                        if (ModConfig.TargetBooks.isEmpty()){
                            // 1個も欲しい本が無いのかい！？
                            // 違う！
                            Component msg = Component.translatable("message.autolibrarian.no_target_book");
                            player.sendSystemMessage(Component.literal(AutoLibrarian.ModPreFix + msg.getString()));
                            mc.player.playSound(SoundEvents.ANVIL_PLACE, 1.0f, 1.0f); // 「金床の音」
                            return;
                        }
                        // ホットバーのアイテムが「書見台（LECTERN）」かチェック
//                        if (!LecternChecker(mc)) {
//                            // 違う！
//                            Component msg = Component.translatable("message.autolibrarian.no_lectern");
//                            player.sendSystemMessage(Component.literal(AutoLibrarian.ModPreFix + msg.getString()));
//                            mc.player.playSound(SoundEvents.ANVIL_PLACE, 1.0f, 1.0f); // 「金床の音」
//                            return;
//                        }
                        // 書見台を持っているか、ホットバーにある！
                    }
                }
            }

            // 設定キーを押した場合
            while (CONFIG_KEY.consumeClick()){
                // マイクラゲーム本体を取得
                Minecraft mc = Minecraft.getInstance();
                mc.player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0f, 1.0f); // SE
                // クライアント側で画面を開く
                net.minecraft.client.Minecraft.getInstance().setScreen(new ConfigScreen());
            }
        }

        // Nキーが押されて自動化をONにしようとした時の処理
        public static boolean LecternChecker(Minecraft mc) {
            // --- 1. メインハンドのチェック ---
            ItemStack mainHandStack = mc.player.getMainHandItem();

            if (mainHandStack.is(Items.LECTERN)) {
                // 書見台を持っているので正常スタート！
                return true;
            } else {
                // --- 2. 持っていない場合：ホットバー内から自動で探して持たせる試み ---
                int lecternSlot = -1;
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = mc.player.getInventory().getItem(i);
                    if (stack.is(Items.LECTERN)) lecternSlot = i; //見つかったらインデックス番号を入れておく
                }


                if (lecternSlot != -1) {
                    // ホットバーに書見台があったので、そのスロットに持ち替える！
                    mc.player.getInventory().selected = lecternSlot;
                    return true;
                } else return false;
            }
        }
    }
}