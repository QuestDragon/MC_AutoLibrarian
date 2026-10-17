package com.example.autolibrarian;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchantmentListWidget extends ObjectSelectionList<EnchantmentListWidget.Entry> {

    public EnchantmentListWidget(Minecraft minecraft, int width, int height, int top, int bottom, int itemHeight) {
        super(minecraft, width, height, top, bottom, itemHeight);
        // マイクラ標準の上下土背景ヘッダー・フッターを無効化！
        this.setRenderHeader(false, 0);
        this.setRenderBackground(false); // または setRenderTopAndBottom(false);
    }

    // エントリの追加メソッドなどをここに定義していきます

    // 1行分の描画を担当するインナークラス
    public static class Entry extends ObjectSelectionList.Entry<Entry> {
        private final Component name;
        private final String id;
        private final Runnable onClick;

        public Entry(Component name, String id, Runnable onClick) {
            this.name = name;
            this.id = id;
            this.onClick = onClick;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            // エンチャント本のアイコンを描画
            guiGraphics.renderItem(new ItemStack(Items.ENCHANTED_BOOK), left + 5, top + 2);

            // エンチャント名を描画
            guiGraphics.drawString(Minecraft.getInstance().font, this.name, left + 30, top + 2, 0xFFFFFFFF);

            // IDや詳細情報を薄い灰色で描画
            guiGraphics.drawString(Minecraft.getInstance().font, this.id, left + 30, top + 13, 0x88888888);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) { // 左クリック
                this.onClick.run();
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            return this.name;
        }


    }

    // protected な clearEntries を public として開放する
    @Override
    public void clearEntries() {
        super.clearEntries();
    }

    // （ついでに addEntry も必要に応じて public メソッドとして使えるように確認）
    @Override
    public int addEntry(Entry entry) {
        return super.addEntry(entry);
    }

    @Override
    protected void renderHeader(GuiGraphics guiGraphics, int x, int y) {
        // ヘッダーの土背景を描画しない
    }

    @Override
    protected void renderDecorations(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // フッター等の余計な装飾・背景描画を行わない
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        // リスト独自の土背景を描画しない
    }
}