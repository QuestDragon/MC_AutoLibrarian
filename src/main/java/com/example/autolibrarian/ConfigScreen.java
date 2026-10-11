package com.example.autolibrarian;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector2i;

import java.awt.*;
import java.util.List;

public class ConfigScreen extends Screen {

    // 現在開いているタブ（0: 基本設定, 1: エンチャント設定）
    private int currentTab = 0;

    // リストを保持する変数
    private EnchantmentListWidget enchantmentList;

    // 現在「全エンチャント選択中(true)」か「指定リスト表示中(false)」かを切り替えるフラグ
    private boolean isSelectingMode = false;

    // 選択中のエンチャントID（例: "minecraft:mending"）
    private String selectedEnchantmentId = "";


    public ConfigScreen() {
        super(Component.translatable("gui.autolibrarian.config.title")); //タイトル表示部分。この位置はマイクラ側で制御されるので表示する文字だけ指定。
    }

    private void refreshListItems() {
        if (this.enchantmentList == null) return;
        this.enchantmentList.clearEntries(); // 一旦クリア

        if (isSelectingMode) {
            // 【全リストモード】マイクラに存在する全エンチャントを表示
            for (var entry : ForgeRegistries.ENCHANTMENTS.getEntries()) {
                var location = entry.getKey().location();
                String id = location.toString();
                Enchantment ench = entry.getValue();

                // 表示名（Component）
                Component name = ench.getFullname(1); // 例: "修繕 I" や "Mending"

                // 行を追加
                this.enchantmentList.addEntry(new EnchantmentListWidget.Entry(name, id, () -> {
                    // クリックされた時の処理
                    this.selectedEnchantmentId = id;
                }));
            }
        } else {
            // 【指定リストモード】ModConfig.TargetBooks に登録されている本だけを表示
            Component Label_Lv = Component.translatable("gui.autolibrarian.target_books.enchant_level");
            Component Label_Price = Component.translatable("gui.autolibrarian.target_books.sell_price");

            // 指定済みエンチャント本を順番に変数：bookへ…
            for (ModConfig.TargetBook book : ModConfig.TargetBooks) {
                // エンチャント本の表記をIDから名称に変更するコード：
                Component displayName;
                ResourceLocation location = ResourceLocation.tryParse(book.enchantmentId);
                // Forge のレジストリからエンチャントを取得
                Enchantment enchantment = (location != null) ? ForgeRegistries.ENCHANTMENTS.getValue(location) : null;

                if (enchantment != null) {
                    // 取得できた場合：言語設定に基づいた名前 (例: "修繕 I" や "Mending I") を生成
                    displayName = enchantment.getFullname(book.level);
                } else {
                    // 万が一見つからない場合のフォールバック（生のID）
                    displayName = Component.literal(book.enchantmentId + " Lv." + book.level);
                }

                String EnchNameColor = displayName.getString().contains("Curse") ? "§c" : "";

                // 実際にリストに表示される内容
                Component name = Component.literal(EnchNameColor + displayName.getString() + "§f / " + Label_Price.getString() + "§a" +  book.maxPrice);

                // 最後にリストオブジェクトへ必要データと共に渡す。そうすると選択状態になるので、内部変数へ情報を入れておく。
                this.enchantmentList.addEntry(new EnchantmentListWidget.Entry(name, book.enchantmentId, () -> {
                    this.selectedEnchantmentId = book.enchantmentId;
                    this.selectedLevel = book.level;
                    this.selectedMaxPrice = book.maxPrice;
                }));

                // リスト項目選択時（refreshListItems内）や Add ボタン押下時 でも最大レベルを超えないようSelectedLevelを変えておく…。
                int maxLv = getSelectedEnchantmentMaxLevel();
                if (this.selectedLevel > maxLv) this.selectedLevel = maxLv;
            }
        }
    }

    // 選択中のエンチャントの最大レベルを取得するヘルパーメソッド
    private int getSelectedEnchantmentMaxLevel() {
        if (this.selectedEnchantmentId != null && !this.selectedEnchantmentId.isEmpty()) {
            ResourceLocation location = ResourceLocation.tryParse(this.selectedEnchantmentId);
            if (location != null) {
                Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(location);
                if (enchantment != null) {
                    return enchantment.getMaxLevel(); // 修繕: 1, 効率強化: 5 など
                }
            }
        }
        return 5; // 選択されていない場合は安全のために仮の上限値（または 5）
    }

    // ちなみにUIのDrawStringはRenderメソッドに入っているので、常にSelectedLevelやSelectedMaxPriceを監視している。だから気にしなくてOK！
    // レベル変更用の処理
    private void updateSelectedLevel(int delta) {
        int maxLevel = getSelectedEnchantmentMaxLevel();
        // 1 〜 maxLevel の範囲内にクランプ（制限）する！
        this.selectedLevel = Math.min(maxLevel, Math.max(1, this.selectedLevel + delta));

        // 指定リストモードかつ項目が選択中なら、設定データもリアルタイム更新
        if (!this.isSelectingMode && !this.selectedEnchantmentId.isEmpty()) {
            for (ModConfig.TargetBook book : ModConfig.TargetBooks) {
                if (book.enchantmentId.equals(this.selectedEnchantmentId)) {
                    book.level = this.selectedLevel;
                    // 設定ファイルに保存
                    ModConfig.save();
                    break;
                }
            }
            refreshListItems(); // リスト上の表示名（Lv表記）も更新
        }
    }

    // 価格変更用の処理
    private void updateSelectedPrice(int delta) {
        this.selectedMaxPrice = Math.min(90, Math.max(1, this.selectedMaxPrice + delta)); // 1〜90の範囲に制限 ※トレジャーエンチャント理論最大値（Lv.3 = 45 x 2）

        // 指定リストモードかつ項目が選択中なら、設定データもリアルタイム更新
        if (!this.isSelectingMode && !this.selectedEnchantmentId.isEmpty()) {
            for (ModConfig.TargetBook book : ModConfig.TargetBooks) {
                if (book.enchantmentId.equals(this.selectedEnchantmentId)) {
                    book.maxPrice = this.selectedMaxPrice;
                    // 設定ファイルに保存
                    ModConfig.save();
                    break;
                }
            }
            refreshListItems(); // リスト上の表示名（Price表記）も更新
        }
    }

    @Override
    protected void init() {
        super.init();

        int DoneButtonWidth = 200;
        int buttonWidth = 120;
        int buttonHeight = 20;
        int centerX = this.width / 2; //画面幅の割る2で中心X

        Component GeneralSettingsButtonLabel = Component.translatable("gui.autolibrarian.config.general");
        Component TargetBookButtonLabel = Component.translatable("gui.autolibrarian.config.target_book");
        if (currentTab < 1){
            GeneralSettingsButtonLabel = Component.literal("§e" + Component.translatable("gui.autolibrarian.config.general").getString());
        }else{
            TargetBookButtonLabel = Component.literal("§e" + Component.translatable("gui.autolibrarian.config.target_book").getString());
        }

        // --- タブボタンの配置（画面上部） ---
        // 事前情報：ボタンのAnchorはLeftになっている。Xの位置はLeft面の位置として考えれば良い。
        // 図解：▼が指定位置。
        // [　　▼General　-余白5-　Center　-余白5-　▼Target Books　　]

        // 基本設定タブ
        this.addRenderableWidget(Button.builder(GeneralSettingsButtonLabel,button -> selectTab(0)
        ).bounds(centerX - buttonWidth - 5, 30, buttonWidth, buttonHeight).build());
        // X:中心位置からボタンの幅分左に移動させ、さらに余白として5移動させる。
        // Y:画面上端から30の位置。
        // あとはボタンのサイズを幅と高さの順で指定すればOK。

        // エンチャント設定タブ
        this.addRenderableWidget(Button.builder(TargetBookButtonLabel,button -> selectTab(1)
        ).bounds(centerX + 5, 30, buttonWidth, buttonHeight).build());
        // X:中心位置から余白の5だけ左に移動させる。ボタンの左端が中心から5移動したところになる。
        // Y:画面上端から30の位置。
        // あとはボタンのサイズを幅と高さの順で指定すればOK。

        // --- 画面下の閉じるボタン ---
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> this.onClose() // 画面を閉じる
        ).bounds(centerX - (DoneButtonWidth / 2), this.height - 30, DoneButtonWidth, buttonHeight).build());
        // X:中心位置から50だけ左に移動させる。ボタンの左端が中心から50移動したところになる。
        // Y:画面下端から30上の位置。（Heightなら画面下端を指定できる。例えば1280x720なら720がHeightの値。画面上端を0、下端を720としてみてるから、Heightなら一発…そういうこと。）
        // あとはボタンのサイズを幅と高さの順で指定すればOK。

        // タブごとのコンポーネント初期化
        initTabComponents();
    }

    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝

    private void selectTab(int tabIndex) {
        this.currentTab = tabIndex;
        // タブが切り替わったら画面要素を再構築
        this.clearWidgets();
        this.init();
    }

    // General Settings用の変数。
    // Finalって？：いわゆるConstのようなもの。1度だけしか代入できない変数。今回の下の例ではもう代入しちゃってるのでこの先値の変更は出来ない。
    private final Vector2i AxeProtectionValueLabelPos = new Vector2i(20,70 + (25 * 4)); // 座標型だけどXYだしちょうどいいよね！ｗ2iはIntという意味。2fならFloat型になる。DrawStringはIntを要求するので2i。

    // Target Books用の変数。
    private int selectedLevel = 1;
    private int selectedMaxPrice = 20;

    // 呼び出し元はInitメソッド。部品と処理は別々にしておけばわかりやすいもんね。
    private void initTabComponents() {
        // Tips: RenderStringはRenderメソッドに書く。ボタン類はInitTabComponentsに書く。
        // それぞれの違いは、Renderは毎回呼ばれるのに対し、InitTab（Initメソッド）は1回しか呼ばれない。　毎回呼ばれるRenderにボタンを置いてしまったらどうなるか…想像つきますよね？

        int centerX = this.width / 2;
        int ComponentStartY = 70;
        int ComponentWidth = 200;
        int ComponentHeight = 20;
        int ComponentMarginY = 25;
        int ComponentMarginX = 10;

        // X位置のヒント：
        // 中央揃えなら… centerX - ComponentWidth / 2
        // 2個並べるなら下記2つ：
        // centerX - (ComponentWidth + ComponentMarginX) ※中心からボタン幅だけ抜くと中心を軸に右揃えになるので、そこからMargin引けば良い。
        // centerX + ComponentMarginX ※中心指定すると中心を軸として左揃えになるので、そこにMargin足せば良い。

        if (currentTab == 0) {
            // TODO: 基本設定（SEのON/OFF、耐久値制限などのボタン）を追加する場所

            // --- General タブの要素群 ---

            // ① Status表示のON/OFF
            this.addRenderableWidget(
                    CycleButton.booleanBuilder(Component.translatable("general.autolibrarian.on"), Component.translatable("general.autolibrarian.off"))
                            .withInitialValue(ModConfig.ShowStatus)
                            .withTooltip(mode -> Tooltip.create(Component.translatable("gui.autolibrarian.config_tips.show_status")))
                            .create(centerX - ComponentWidth / 2, ComponentStartY, 200, ComponentHeight, Component.translatable("gui.autolibrarian.config.show_status"), (b, val) -> {
                                ModConfig.ShowStatus = val;
                                // 設定ファイルに保存
                                ModConfig.save();
                            })
            );
            // 168937＝X:中心位置から100だけ左に移動させる。ラベルの左端が中心から100移動したところになる。
            // 168938＝Y:画面上端からComponentStartY だけ下に移動した位置。
            // あとはボタンのサイズを幅（168939）と高さ（168940）の順で指定すればOK。

            // ① 理論値強調表示のON/OFF ※説明が長くなるので斧耐久値指定ラベルと同じツールチップ表示を採用しています。
            this.addRenderableWidget(
                    CycleButton.booleanBuilder(Component.translatable("general.autolibrarian.on"), Component.translatable("general.autolibrarian.off"))
                            .withInitialValue(ModConfig.UsingColoredCost)
                            .create(centerX - ComponentWidth / 2, ComponentStartY + (ComponentMarginY * 1), ComponentWidth, ComponentHeight, Component.translatable("gui.autolibrarian.config.using_colored_cost"), (b, val) -> {
                                ModConfig.UsingColoredCost = val;
                                // 設定ファイルに保存
                                ModConfig.save();
                            })
            );

            // ① 斧の使用ON/OFF
            this.addRenderableWidget(
                    CycleButton.booleanBuilder(Component.translatable("general.autolibrarian.on"), Component.translatable("general.autolibrarian.off"))
                            .withInitialValue(ModConfig.UsingAxe)
                            .withTooltip(mode -> Tooltip.create(Component.translatable("gui.autolibrarian.config_tips.using_axe")))
                            .create(centerX - ComponentWidth / 2, ComponentStartY + (ComponentMarginY * 2), ComponentWidth, ComponentHeight, Component.translatable("gui.autolibrarian.config.using_axe"), (b, val) -> {
                                ModConfig.UsingAxe = val;
                                // 設定ファイルに保存
                                ModConfig.save();
                            })
            );

            // 斧の保護モード切り替えボタン
            this.addRenderableWidget(
                    CycleButton.builder(AutoRerollHandler.AxeProtectionMode::getDisplayName)
                            .withValues(AutoRerollHandler.AxeProtectionMode.values()) // 【ALL -> ENCHANTED -> OFF】の順で巡回。Enum.ValuesでEnumの値全てをコレクションで得られる。
                            .withInitialValue(ModConfig.AxeProtect) // 現在の設定値
                            .withTooltip(mode -> Tooltip.create(Component.translatable("gui.autolibrarian.config_tips.axe_protection")))
                            .create(centerX - ComponentWidth / 2, ComponentStartY + (ComponentMarginY * 3), ComponentWidth, ComponentHeight, // X, Y, 幅, 高さ
                                    Component.translatable("gui.autolibrarian.config.axe_protection"), // ラベル名
                                    (button, newMode) -> {
                                        // ボタンが押されてモードが変わった時の処理
                                        ModConfig.AxeProtect = newMode;
                                        // 設定ファイルに保存
                                        ModConfig.save();
                                    }
                            )
            );

            // ② 斧の最低耐久値設定…のボタン部分 ※ラベルはRenderメソッドに書いてあります。ラベルY位置を変えるにはAxeProtectionValueLabelPosの倍率を変えてください。 ※原則AxeYと同じ倍率値を使う
            int axeY = ComponentStartY + (ComponentMarginY * 4); // ボタンY位置
            this.addRenderableWidget(Button.builder(Component.literal("-"), b -> {
                if (ModConfig.MinAxeDurability > 0) {
                    ModConfig.MinAxeDurability--;
                    // 設定ファイルに保存
                    ModConfig.save();
                }
            }).bounds(centerX + AxeProtectionValueLabelPos.x, axeY, ComponentWidth / 10, ComponentHeight).build());

            this.addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                ModConfig.MinAxeDurability++;
                // 設定ファイルに保存
                ModConfig.save();
            }).bounds((int) (centerX + (AxeProtectionValueLabelPos.x * 2.25)), axeY, ComponentWidth / 10, ComponentHeight).build());

            // エンチャント本が見つかった際の処理切り替えボタン
            this.addRenderableWidget(
                    CycleButton.builder(AutoRerollHandler.FoundAfterOperations::getDisplayName)
                            .withValues(AutoRerollHandler.FoundAfterOperations.values()) // 説明は斧の保護モード参照
                            .withInitialValue(ModConfig.AfterOperation) // 現在の設定値
                            .withTooltip(mode -> Tooltip.create(Component.translatable("gui.autolibrarian.config_tips.foundafter")))
                            .create(
                                    centerX - ComponentWidth / 2, ComponentStartY + (ComponentMarginY * 5), ComponentWidth, ComponentHeight, // X, Y, 幅, 高さ
                                    Component.translatable("gui.autolibrarian.config.foundafter"), // ラベル名
                                    (button, newMode) -> {
                                        // ボタンが押されてモードが変わった時の処理
                                        ModConfig.AfterOperation = newMode;
                                        // 設定ファイルに保存
                                        ModConfig.save();
                                    }
                            )
            );
        }
        else if (currentTab == 1) {
            // TODO: エンチャント本設定（Wurst風のリストや数値設定ボタン）を追加する場所
            int top = 60;              // リストの上端（タブボタンの下）
            int bottom = this.height - 90; // リストの下端（Lv/Priceコントロールの上）

            // 1. スクロールリストの生成
            this.enchantmentList = new EnchantmentListWidget(
                    this.minecraft,
                    this.width,
                    this.height,
                    top,
                    bottom,
                    24 // 1行あたりの高さ(px)
            );

            // 2. リストに要素を詰める（モードに応じて分岐）
            refreshListItems();

            // ★重要★ addRenderableWidget(this.enchantmentList) は呼ばない！
            // (addWidget だけ呼んでおくと、リストのクリック・スクロール操作だけが受け取れます)
            this.addWidget(this.enchantmentList);


            // ＝＝＝＝＝以下ボタン部分＝＝＝＝＝
            int bottomY = this.height - 80;
            // レベルと価格。ラベル幅のうち大きい方を採用。
            int Label_Width = ComponentMarginX + Math.max(font.width(Component.translatable("gui.autolibrarian.target_books.enchant_level").getString() + selectedLevel), font.width(Component.translatable("gui.autolibrarian.target_books.sell_price").getString() + selectedMaxPrice));

            // ComponentWidth / 8 で25。（ComponentWidthが200の場合）
            // ① Level設定 ( [-] [+] ボタン)
            this.addRenderableWidget(Button.builder(Component.literal("-1"), b -> {
                updateSelectedLevel(-1);
            }).bounds(centerX + ComponentMarginX + Label_Width, bottomY, ComponentWidth / 8, ComponentHeight).build());

            this.addRenderableWidget(Button.builder(Component.literal("+1"), b -> {
                updateSelectedLevel(1);
            }).bounds(centerX + (ComponentMarginX * 2) + Label_Width + (ComponentWidth / 10), bottomY, ComponentWidth / 8, ComponentHeight).build());


            // ② Max Price設定 ( [-] [+] ボタン)
            this.addRenderableWidget(Button.builder(Component.literal("-10"), b -> {
                updateSelectedPrice(-10);
            }).bounds(centerX + ComponentMarginX + Label_Width, bottomY + 25, ComponentWidth / 8, ComponentHeight).build());

            this.addRenderableWidget(Button.builder(Component.literal("-1"), b -> {
                updateSelectedPrice(-1);
            }).bounds(centerX + (ComponentMarginX * 2) + Label_Width + ((ComponentWidth / 10) * 1), bottomY + 25, ComponentWidth / 8, ComponentHeight).build());

            this.addRenderableWidget(Button.builder(Component.literal("+1"), b -> {
                updateSelectedPrice(1);
            }).bounds(centerX + (ComponentMarginX * 3) + Label_Width + ((ComponentWidth / 10) * 2), bottomY + 25, ComponentWidth / 8, ComponentHeight).build());

            this.addRenderableWidget(Button.builder(Component.literal("+10"), b -> {
                updateSelectedPrice(10);
            }).bounds(centerX + (ComponentMarginX * 4) + Label_Width + ((ComponentWidth / 10) * 3), bottomY + 25, ComponentWidth / 8, ComponentHeight).build());

            // ===============================================================
            // 三項演算子。SelectingModeがTrueならBack、FalseならAddが入るように。
            Component LeftUpButtonLabel = isSelectingMode ? Component.translatable("general.autolibrarian.back") : Component.translatable("general.autolibrarian.add_new");
            Component LeftDownButtonLabel = isSelectingMode ? Component.translatable("general.autolibrarian.add") : Component.translatable("general.autolibrarian.remove");

            // ③ Add / Remove ボタン
            this.addRenderableWidget(Button.builder(LeftUpButtonLabel, b -> {
                isSelectingMode = !isSelectingMode; //反転
                // 指定値の初期化
                selectedLevel = 1;
                selectedMaxPrice = 20;
                refreshListItems();

                // 画面を更新してボタン表示などを切り替え
                this.clearWidgets();
                this.init();
            }).bounds(centerX - 105, bottomY, ComponentWidth / 2, ComponentHeight).build());

            this.addRenderableWidget(Button.builder(LeftDownButtonLabel, b -> {
                if (this.isSelectingMode) {
                    // 【Add 処理】選択中のエンチャントを ModConfig に追加
                    if (!this.selectedEnchantmentId.isEmpty()) {
                        // リスト項目選択時（refreshListItems内）や Add ボタン押下時 でも最大レベルを超えないようSelectedLevelを変えておく…。
                        int maxLv = getSelectedEnchantmentMaxLevel();
                        if (this.selectedLevel > maxLv) this.selectedLevel = maxLv;

                        // 重複追加を避けるためのチェック＆追加処理 ※過去
                        // ModConfig.TargetBooks.removeIf(bb -> bb.enchantmentId.equals(this.selectedEnchantmentId));
                        // ModConfig.TargetBooks.add(new ModConfig.TargetBook(this.selectedEnchantmentId, this.selectedLevel, this.selectedMaxPrice));

                        boolean exactMatchExists = false;

                        for (ModConfig.TargetBook book : ModConfig.TargetBooks) {
                            if (book.enchantmentId.equalsIgnoreCase(selectedEnchantmentId) && book.level == selectedLevel) {
                                // 同じIDかつ同じレベルのものが既にある場合は価格だけ更新
                                book.maxPrice = selectedMaxPrice;
                                exactMatchExists = true;
                                break;
                            }
                        }

                        // 同じIDであってもレベルが違えば（例: Silk Touch Lv.1 と Lv.2 など）、別のエントリとして新規追加を許可する
                        if (!exactMatchExists) {
                            ModConfig.TargetBooks.add(new ModConfig.TargetBook(selectedEnchantmentId, selectedLevel, selectedMaxPrice));
                        }

                        // 設定ファイルに保存
                        ModConfig.save();

                        // 追加完了したら登録済みリストモードに戻す
                        this.isSelectingMode = false;
                        refreshListItems();
                        this.clearWidgets();
                        this.init();
                    }
                } else {
                    // 【Remove 処理】選択中の本を ModConfig から削除
                    if (!this.selectedEnchantmentId.isEmpty()) {
                        ModConfig.TargetBooks.removeIf(bb -> bb.enchantmentId.equals(this.selectedEnchantmentId));
                        this.selectedEnchantmentId = "";

                        // 設定ファイルに保存
                        ModConfig.save();

                        refreshListItems();
                    }
                }

                // 画面を更新してボタン表示などを切り替え
                this.clearWidgets();
                this.init();
            }).bounds(centerX - 105, bottomY + 25, ComponentWidth / 2, ComponentHeight).build());
        }

    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        Component Label_DurabilityValue = Component.translatable("gui.autolibrarian.config.axe_protection_value");
        int AxeProtectionValueLabelStartX = centerX - font.width(Label_DurabilityValue) - 10;

        // 背景を暗転描画
        this.renderBackground(guiGraphics);

        // 2. リストを単体で先に描画（ボタンより下に敷くため）
        if (currentTab == 1 && this.enchantmentList != null) {
            this.enchantmentList.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        // Tips: RenderStringはRenderメソッドに書く。ボタン類はInitTabComponentsに書く。
        // それぞれの違いは、Renderは毎回呼ばれるのに対し、InitTab（Initメソッド）は1回しか呼ばれない。　毎回呼ばれるRenderにボタンを置いてしまったらどうなるか…想像つきますよね？
        // メインタイトルの描画
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);

        // General Settings
        if (currentTab == 0) {
            // ② 斧の最低耐久値設定…のラベル部分
            // AxeProtectionValueLabelPosは上の方に書いてあるよ～
            // 処理の関係上ここに使ってる変数はメソッド名の下に書いてあるよ～
            guiGraphics.drawString(this.font, Component.literal(Label_DurabilityValue.getString() + ": §6" + ModConfig.MinAxeDurability),AxeProtectionValueLabelStartX, AxeProtectionValueLabelPos.y + 5, 0xFFEEEEEE);

        }
        else {
            // Target Books のラベル描画部分
            int bottomY = this.height - 80;

            // 2. リストを手動で単独描画（ボタン描画より絶対に前に実行する）
            if (this.enchantmentList != null) this.enchantmentList.render(guiGraphics, mouseX, mouseY, partialTick);

            // Selectedシリーズの変数はIntだが、ComponentをString変換しているので同じようにString変換してくれる。だからエラーにならない。
            // もしString変換されていない場合はInteger.tostring()で変換してあげる必要がある。
            // "Level: 1" の描画
            Component Label_Lv = Component.translatable("gui.autolibrarian.target_books.enchant_level");
            guiGraphics.drawString(this.font, Component.literal(Label_Lv.getString() + "§d" + selectedLevel), centerX + 5, bottomY + 5, 0xFFFFFFFF);

            // "Max price: 20" の描画（エメラルドアイコンっぽく緑色 0x55FF55 にしてもお洒落です）
            Component Label_Price = Component.translatable("gui.autolibrarian.target_books.sell_price");
            guiGraphics.drawString(this.font, Component.literal(Label_Price.getString() +"§a" + selectedMaxPrice), centerX + 5, bottomY + 30, 0xFFFFFFFF);

            // 画面に登録されているすべてのボタン（Widget）を、土背景の「上から」一番手前に描画する！
            for (var widget : this.children()) {
                if (widget instanceof net.minecraft.client.gui.components.Renderable renderable) {
                    // リスト自体は上で個別に描画済みなので二重描画を避ける
                    if (widget != this.enchantmentList) {
                        renderable.render(guiGraphics, mouseX, mouseY, partialTick);
                    }
                }
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // General Settingsの場合の特殊処理。Renderしたあとじゃ無いと動かないから…。
        if (currentTab == 0) {
            // 3. マウスが耐久値設定のテキストの上に乗っているか判定してツールチップを描画 ※9はラベルの高さ。大体だけど…。
            if (mouseX >= AxeProtectionValueLabelStartX && mouseX <= centerX &&
                    mouseY >= AxeProtectionValueLabelPos.y && mouseY <= AxeProtectionValueLabelPos.y + 10) {

                // 画面サイズや \n に合わせて自動的に折り返し・改行したリストに変換 ※WithTooltipではなく、単体のRenderToolTipなので改行はこちらで行わないと画面からはみ出る。
                Component AxeProtectionValueTips = Component.translatable("gui.autolibrarian.config_tips.axe_protection_value");
                var splitTooltip = this.minecraft.font.split(AxeProtectionValueTips, 200); // 200は最大横幅(px)
                guiGraphics.renderTooltip(this.font,splitTooltip,mouseX,mouseY);
            }

            // .create(centerX - ComponentWidth / 2, ComponentStartY + (ComponentMarginY * 1), ComponentWidth, ComponentHeight, Component.translatable("gui.autolibrarian.config.using_colored_cost"), (b, val) -> {

            // ボタンのWidth、200の半分で100。
            // ComponentStartYが70、Marginが25なので足して95。 Heightは20なのでYの終わりは120。
            if (mouseX >= centerX - 100 && mouseX <= centerX + 100 &&
                    mouseY >= 95 && mouseY <= 95 + 20) {

                // 画面サイズや \n に合わせて自動的に折り返し・改行したリストに変換 ※WithTooltipではなく、単体のRenderToolTipなので改行はこちらで行わないと画面からはみ出る。
                Component ColorCostTips = Component.translatable("gui.autolibrarian.config_tips.using_color_cost");
                var splitTooltip = this.minecraft.font.split(ColorCostTips, 200); // 200は最大横幅(px)
                guiGraphics.renderTooltip(this.font,splitTooltip,mouseX,mouseY);
            }
        }

    }

    @Override
    public boolean isPauseScreen() {
        return false; // 開いている間もシングルプレイで時間を止めない場合は false
    }
}