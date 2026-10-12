package com.example.autolibrarian;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@Mod.EventBusSubscriber(modid = "autolibrarian", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class AutoRerollHandler {

    public enum AxeProtectionMode {
        ALL("gui.autolibrarian.config.axe_protection_all"),         // 全て
        ENCHANTED("gui.autolibrarian.config.axe_protection_enchanted"), // エンチャント済みのみ
        OFF("general.autolibrarian.off");          // オフ

        private final String translationKey;

        // Enumの本体。
        AxeProtectionMode(String translationKey) {
            this.translationKey = translationKey;
        }

        public Component getDisplayName() {
            return Component.translatable(this.translationKey);
        }
    }

    public enum FoundAfterOperations {
        LOW_PRICE("gui.autolibrarian.config.foundafter_price"),         // 価格を現在の取引価格から1下げたものに変更
        REMOVE("gui.autolibrarian.config.foundafter_remove"), // リストから除去
        NOTHING("general.autolibrarian.off");          // なにもしない

        private final String translationKey;

        // Enumの本体。
        FoundAfterOperations(String translationKey) {
            this.translationKey = translationKey;
        }

        public Component getDisplayName() {
            return Component.translatable(this.translationKey);
        }
    }

    public enum RerollState {
        DESTROY("status.autolibrarian.destroy"),
        WAIT_DESTROY("status.autolibrarian.wait_destroy"),
        WAIT_UNEMPLOYED("status.autolibrarian.wait_unemployed"),
        PLACE("status.autolibrarian.place"),
        WAIT_LIBRARIAN("status.autolibrarian.wait_librarian"),
        TALK("status.autolibrarian.talk"),
        CHECK_TRADE("status.autolibrarian.check_trade");

        private final String TranslationKey;

        // Enumの本体。
        RerollState(String TranslationKey) {
            this.TranslationKey = TranslationKey;
        }

        public String getTranslationKey() {
            return this.TranslationKey;
        }
    }

    // グローバル変数。何度も呼ばれるOnClientTickはこれを見ながら今の状態を判断し、動く。
    private static boolean FirstEnabled = false;
    private static boolean BreakedLectern = false;
    private static RerollState State = RerollState.DESTROY;
    private static int timer = 0;
    private static BlockPos targetPos = null;

    // ディレイ設定（Tick単位：20Tick = 1秒）
    // サーバーのラグに合わせて数値を調整してください
    private static final int DELAY_TICKS = 10; // 0.5秒間隔

    // （アクションバー）にステータスを表示するメソッド
    private static void setState(Minecraft mc, RerollState newState, boolean IsSilent) {
        State = newState; // Stateチェンジ

        // ステータスの表示がONなら表示処理をする
        if (mc.player != null && ModConfig.ShowStatus && !IsSilent) {
            // 第2引数の true が「アクションバー（持っているアイテム名表示場所）に表示する」という指定です！
            Component msg = Component.translatable(newState.TranslationKey); //Enumに指定したKeyを使ってLang Jsonより取得
            mc.player.displayClientMessage(
                    Component.literal(AutoLibrarian.ModPreFix + "§f" + msg.getString()),
                    true
            );
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // 処理の終了フェーズのみ実行、かつ機能がONの時だけ動作
        // 終了フェーズとは：1Tickの間で、そのTickの終了部分をTickEvent.Phase.ENDで指すことができる。
        // そのTickの終了の瞬間とこのイベントハンドラのPhaseが異なる場合（終了の瞬間でない場合）は何もしないという処理。
        // かかないと二重でこの処理が呼び出され、バグの原因となる。そのためその回避用にこのIFがある。
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || !KeyHandler.Get_isEnabled()) {
            if (!KeyHandler.Get_isEnabled()) {
                FirstEnabled = true; //次回Enabled時に初回限定処理がこれで動く。
                if (State != RerollState.DESTROY) setState(mc, RerollState.DESTROY, true);
            }
            return;
        }
        if (mc.player == null || mc.level == null) return;

        // タイマー処理 ※カウントダウン
        if (timer > 0) {
            timer--;
            return;
        }

        // タイマーが0になると処理開始

        // 1. targetPos が未設定なら、周囲から「書見台」または「設置用スペース」を探す
        if (targetPos == null) {
            // TargetPos＝書見台または設置スペースの位置
            targetPos = findLecternOrSpace(mc);
        }

        if (targetPos == null) {
            Component msg = Component.translatable("message.autolibrarian.not_enough_space"); //Keyを使ってLang Jsonより取得
            mc.player.sendSystemMessage(Component.literal(AutoLibrarian.ModPreFix + msg.getString()));
            KeyHandler.Set_isEnabled(mc, false, false);
            // 周囲に書見台も置けるスペースもない場合は処理をスキップ
            return;
        }

        // 書見台または設置スペースの情報
        BlockState currentState = mc.level.getBlockState(targetPos);

        switch (State) {
            case DESTROY: // 【破壊フェーズ】書見台が存在するなら壊す
                // 設置スペースの場所が「書見台」かチェック。Level（ワールド）に対してブロック位置情報を渡し、確認処理。
                if (currentState.is(Blocks.LECTERN)) {
                    // 書見台なら破壊
                    // なぜGamemode?：ゲームモードに基づいてプレイヤーの処理信号を送信する役割を持っているため。まあ、ある意味Gamemode。
                    // mc.gameMode.destroyBlock(targetPos); // 即時破壊 ※クリエイティブ風。ただし仕組みが異なるため正しく検証できない恐れあり。
                    // ホットバーに斧があるかチェック
                    int AxeIndex = InvChanger(mc.player, true, false);
                    if (ModConfig.UsingAxe && FirstEnabled && AxeIndex < 0) {
                        // AL開始時
                        Component msg = Component.translatable("message.autolibrarian.axes_tip"); //Keyを使ってLang Jsonより取得
                        mc.player.sendSystemMessage(Component.literal(AutoLibrarian.ModPreFix + msg.getString()));
                        // 斧はあるけどボロッボロの場合はそのTipsも出す
                        if (AxeIndex < -1) mc.player.sendSystemMessage(Component.translatable("message.autolibrarian.whynotuse_myaxes_tip"));
                    }
                    if (AxeIndex > -1){ //斧シリーズを所有していればそれに持ち替えて
                        mc.player.getInventory().selected = AxeIndex;
                    }
                    mc.gameMode.startDestroyBlock(targetPos, Direction.UP);
                    mc.player.swing(InteractionHand.MAIN_HAND);

                    FirstEnabled = false; //初回限定処理終了
                    setState(mc, RerollState.WAIT_DESTROY, false); // ステータス表示とState Change
                    timer = 2;
                } else if (currentState.is(Blocks.AIR)) {
                    // 既に空気ならそのまま村人チェックフェーズへ進む
                    setState(mc, RerollState.WAIT_UNEMPLOYED, false);
                }
                break;

            case WAIT_DESTROY: // 【破壊確認フェーズ】空気になったか
                if (currentState.is(Blocks.AIR)) {
                    BreakedLectern = true;
                    timer = 2;
                } else {
                    // まだ壊れていなければ再試行
                    // mc.gameMode.destroyBlock(targetPos); // 即時破壊 ※クリエイティブ風。ただし仕組みが異なるため正しく検証できない恐れあり。
                    mc.gameMode.continueDestroyBlock(targetPos, Direction.UP);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                    timer = 2;
                }

                // 書見台を破壊済み、書見台を入手済みで次のフェーズへ
                if (BreakedLectern && InvChanger(mc.player, false, true) > 0) setState(mc, RerollState.WAIT_UNEMPLOYED, false); // 村人チェックフェーズへ

                break;

            case WAIT_UNEMPLOYED: // 【2. 村人の無職チェック】村人がしっかり「無職」に戻るまで待機！
                if (VillagerChecker(mc, "none")) {
                    setState(mc,RerollState.PLACE, false); // 無職を確認できたので「設置」へ！
                    timer = 2;
                } else {
                    // まだ司書の記憶が残っていれば数Tick待機（ここを通過しないと設置しない）
                    timer = 2;
                }
                break;

            case PLACE: // 2. 書見台を設置
                // targetPos（書見台があった場所・設置したい場所）を使う
                BlockHitResult placeHit = new BlockHitResult(
                        // 1. 設置面上のクリック位置（ターゲット座標の中心ベクトル）
                        Vec3.atCenterOf(targetPos),

                        // 2. どの面を右クリックするか（通常は上面 UPPER や側面）
                        Direction.UP,

                        // 3. 設置対象のブロック座標
                        targetPos,

                        // 4. ブロック内部をクリックしたか（基本 false）
                        false
                );

                int LecternIndex = InvChanger(mc.player, false, true);
                if (LecternIndex < 0){
                    Component msg = Component.translatable("message.autolibrarian.missing_lectern"); //Keyを使ってLang Jsonより取得
                    mc.player.sendSystemMessage(Component.literal(AutoLibrarian.ModPreFix + msg.getString()));
                    KeyHandler.Set_isEnabled(mc, false,false);
                    return; //処理終了
                }else{
                    // 持ち替え
                    mc.player.getInventory().selected = LecternIndex;
                }

                // 右クリックで視線の先に書見台設置
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, placeHit);
                // 腕を振るモーション（サーバーに設置アクションを確定させるために重要）
                mc.player.swing(InteractionHand.MAIN_HAND);

                BreakedLectern = false;
                setState(mc, RerollState.WAIT_LIBRARIAN, false); //村人チェック2
                timer = DELAY_TICKS; // 村人が転職するまで少し待つ時間
                break;

            case WAIT_LIBRARIAN: // 【4. 村人の就職チェック】村人が「司書」に転職するまで待機！
                if (VillagerChecker(mc,"librarian")) {
                    setState(mc, RerollState.TALK, false); // 司書になったので「話しかけ（GUI表示）」へ！
                    timer = 2;
                } else {
                    // まだ転職前（無職）なら待機
                    timer = 2;
                }
                break;

            case TALK: // 3. 近くの村人が「司書」になったか確認して話しかける
                // 村人の情報を取得。数珠つなぎで最初に見つけた村人を対象とし、村人がいないならNullとする。
                // getBoundingBoxは、検知範囲を指定できる。今回は5ブロック以内。
                Villager villager = mc.level.getEntitiesOfClass(
                        Villager.class,
                        mc.player.getBoundingBox().inflate(5.0D)
                ).stream().findFirst().orElse(null);

                if (villager != null) {
                    // 村人の職業を取得
                    String profession = villager.getVillagerData().getProfession().toString();

                    // ちゃんと「司書 (librarian)」になっている場合のみ右クリック！
                    if (profession.contains("librarian")) {
                        mc.gameMode.interact(mc.player, villager, InteractionHand.MAIN_HAND);
                        mc.player.swing(InteractionHand.MAIN_HAND); // 腕を振る
                        setState(mc, RerollState.CHECK_TRADE, false);
                        timer = DELAY_TICKS;
                    } else {
                        // まだ無職（または転職前）なら、話しかけずに次のTickまで待機（State 2のまま維持）
                        // ※タイマーを入れて数Tick後に再チェック
                        timer = 5;
                    }
                }
                break;

            case CHECK_TRADE: // 4. 取引画面のチェック
                String enchNameLocalize = ""; // エンチャント表記情報
                int emeraldCost = 0; //価格

                if (mc.screen instanceof MerchantScreen merchantScreen) {
                    MerchantOffers offers = merchantScreen.getMenu().getOffers();
                    int TargetListIndex = -1;
                    boolean foundTarget = false;
                    boolean FoundBook = false;
                    boolean IsTheoreticalCost = false; //理論値であるか
                    boolean IsBottakuriCost = false; //ぼったくられてるか
                    String EnchNameColor = ""; //チャットに通知するためのエンチャント名の文字色

                    for (MerchantOffer offer : offers) {
                        ItemStack resultStack = offer.getResult();
                        // エンチャント本販売していたら
                        if (resultStack.is(Items.ENCHANTED_BOOK)) {
                            FoundBook = true; // エンチャント本が出た
                            // 本に付いているエンチャント一覧を取得
                            var enchantments = EnchantmentHelper.getEnchantments(resultStack);
                            // エメラルドの必要数を取得
                            emeraldCost = offer.getCostA().getCount();

                            for (var entry : enchantments.entrySet()) {
                                // entry.getKey() でエンチャントオブジェクトを取得し、登録名（ID）を判定
                                var enchantment = entry.getKey();
                                int level = entry.getValue();

                                // ① マイクラ公式の表示用テキスト（名前 + レベル）を自動生成！
                                // 例: 「シルクタッチ」「効率強化 V」「修繕」など（言語設定に合わせて自動翻訳）
                                Component enchNameComponent = enchantment.getFullname(level); //情報取得
                                enchNameLocalize = enchNameComponent.getString(); //文字列変換（まとめちゃうとダメ。ちゃんと分ける。）
                                // 例: 変数宣言部で ResourceLocation から正確な ID を取得
                                ResourceLocation location = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
                                String enchId = (location != null) ? location.toString() : ""; // "minecraft:silk_touch" が入る
                                EnchNameColor = enchId.contains("curse") ? "§c" : "§e";

                                // 理論値か確認
                                IsTheoreticalCost = TheoreticalCheck(level, emeraldCost, false, enchantment.isTreasureOnly());
                                IsBottakuriCost =  TheoreticalCheck(level, emeraldCost, true, enchantment.isTreasureOnly());

                                // 設定リスト（ModConfig.TargetBooks）と照合
                                for (int i = 0; i < ModConfig.TargetBooks.size(); i++) {
                                    ModConfig.TargetBook target = ModConfig.TargetBooks.get(i);
                                    // 1. IDが一致するか判定 (全記法 "minecraft:xxx" に対応)
                                    boolean idMatches = target.enchantmentId.equalsIgnoreCase(enchId)
                                            || target.enchantmentId.endsWith(":" + enchId)
                                            || enchId.endsWith(":" + target.enchantmentId);

                                    if (idMatches) {
                                        // 2. レベルが必要指定以上、かつ価格が上限以下かチェック
                                        if (level >= target.level && emeraldCost <= target.maxPrice) {
                                            foundTarget = true;
                                            TargetListIndex = i;

                                            // ログ出力（デバッグ用）
                                            // System.out.println("★ ターゲット本を発見！: " + enchId + " Lv." + enchLevel + " (" + tradePrice + " Emeralds)");
                                            break;
                                        }
                                    }
                                }
                            }
                            break;
                        }
                    }

                    String CostColor = "";
                    if (ModConfig.UsingColoredCost){
                        CostColor = "§a";
                        if (IsTheoreticalCost) CostColor = "§d";
                        if (IsBottakuriCost) CostColor = "§4";
                    }

                    Component ResultMessage = null;
                    Component msg_detail = Component.translatable("message.autolibrarian.trade_detail", EnchNameColor + enchNameLocalize + "§f", CostColor + emeraldCost); //Keyを使ってLang Jsonより取得。引数はjsonに指示した引数番号順で表示される。
                    Component msg_nothing = Component.translatable("message.autolibrarian.trade_nobook"); //Keyを使ってLang Jsonより取得
                    if (FoundBook) ResultMessage = Component.literal(AutoLibrarian.ModPreFix + msg_detail.getString());
                    else ResultMessage = Component.literal(AutoLibrarian.ModPreFix + msg_nothing.getString());
                    mc.player.sendSystemMessage(ResultMessage);

                    if (foundTarget) {
                        // エンチャント本を発見！
                        // mc.player.sendSystemMessage(Component.literal("§aエンチャント本を発見しました！動作成功です！"));
                        mc.player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f); // 挑戦達成SE
                        KeyHandler.Set_isEnabled(mc, false, false); // 自動化停止


                        // --- After Operation の設定に応じた事後処理 ---
                        ModConfig.TargetBook matchedBook = ModConfig.TargetBooks.get(TargetListIndex);
                        switch (ModConfig.AfterOperation) {
                            case LOW_PRICE:
                                // 出た価格よりさらに1安い価格に上限値を自動更新 (最安1エメラルド)
                                matchedBook.maxPrice = Math.max(1, emeraldCost - 1);
                                ModConfig.save(); //保存
                                // System.out.println("★ 次回目標価格を更新: " + matchedBook.maxPrice + " Emeralds");
                                break;

                            case REMOVE:
                                // リストから該当項目を削除
                                ModConfig.TargetBooks.remove(TargetListIndex);
                                ModConfig.save(); //保存
                                // System.out.println("★ ターゲット本をリストから削除しました");
                                break;

                            case NOTHING:
                            default:
                                // 何もしない
                                break;
                        }
                    } else {
                        // 紙やパンなど、本以外なら画面を閉じてやり直し
                        mc.player.closeContainer();
                    }

                    setState(mc, RerollState.DESTROY, false);
                    timer = DELAY_TICKS;
                }
                break;
        }

        // player.sendSystemMessage(Component.literal("§e[AutoLibrarian] Current phase: " + State)); //チャットに出す場合の処理。
    }

    private static BlockPos findLecternOrSpace(Minecraft mc) {
        if (mc.player == null || mc.level == null) return null;

        BlockPos playerPos = mc.player.blockPosition();
        int radius = 3;

        // 【1優先】まずは周囲に既に置かれている「書見台」を探す
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    if (mc.level.getBlockState(checkPos).is(Blocks.LECTERN)) {
                        return checkPos; // 書見台発見！
                    }
                }
            }
        }

// 【2優先】書見台が無い場合、村人の近くにある「置ける空間（AIR）」を探す
        Villager villager = getNearbyVillager(mc);
        if (villager != null) {
            BlockPos vPos = villager.blockPosition();

            // 村人の前後左右上下1ブロックの範囲で「空気が入っていて、下が固いブロック」を探す
            BlockPos[] candidates = {
                    vPos.east(), vPos.west(), vPos.north(), vPos.south(),
                    vPos.east().below(), vPos.west().below(), vPos.north().below(), vPos.south().below()
            };

            for (BlockPos pos : candidates) {
                // その座標が「空気」で、かつ「下（below）が空気ではない（土や木材など）」場所
                if (mc.level.getBlockState(pos).is(Blocks.AIR) &&
                        !mc.level.getBlockState(pos.below()).is(Blocks.AIR)) {
                    return pos; // 設置用スペース発見！
                }
            }
        }

        // それでも見つからない場合は視線の先（hitResult）をフォールバックとして採用
        if (mc.hitResult instanceof BlockHitResult blockHit) {
            BlockPos hitPos = blockHit.getBlockPos();
            // 見ているのが空気（置き場所）か書見台なら採用
            if (mc.level.getBlockState(hitPos).is(Blocks.LECTERN) || mc.level.getBlockState(hitPos).is(Blocks.AIR)) {
                return hitPos;
            }
        }

        return null;
    }

    // 周囲の村人を検索する補助メソッド
    private static Villager getNearbyVillager(Minecraft mc) {
        return mc.level.getEntitiesOfClass(
                Villager.class,
                mc.player.getBoundingBox().inflate(4.0D)
        ).stream().findFirst().orElse(null);
    }

    // 村人が「無職（求職者）」かどうか判定する
    private static boolean VillagerChecker(Minecraft mc, String Prof) {
        Villager villager = getNearbyVillager(mc);
        if (villager == null) return false;

        String prof = villager.getVillagerData().getProfession().toString();
        // 「none」や「none_job」など無職状態かチェック
        return prof.contains(Prof);
    }

    // 斧にチェンジ（なければ-1を返す）
    private static int InvChanger(Player p, boolean IsAxes, boolean IsLectern){
        if (IsAxes && !ModConfig.UsingAxe) return -1; //斧を破壊に使わない場合は-1を即返す
        Inventory inv = p.getInventory();
        Boolean FoundAxe = false;

        for (int i = 0; i < 9; i++) { // ホットバーは0〜8なので、iをそれに限定
            ItemStack stack = inv.getItem(i); //順番にホットバーのアイテム情報を得る
            if (IsAxes){
                // 斧シリーズなら
                if (stack.getItem() instanceof AxeItem) {
                    FoundAxe = true; //斧は見つかった。斧はね。

                    // 耐久値を気にしないならそのまま斧のインデックスを渡す
                    if (ModConfig.AxeProtect == AxeProtectionMode.OFF) return i;
                    // 保護はエンチャント品のみの設定で、エンチャントされていないアイテムの場合はインデックス番号を渡す。（保護対象外のため）
                    if (ModConfig.AxeProtect == AxeProtectionMode.ENCHANTED && !stack.isEnchanted()) return i;

                    // 2. 耐久値のチェック =ここから先は保護対象品=
                    if (stack.isDamageableItem()) {
                        int remaining = stack.getMaxDamage() - stack.getDamageValue();

                        // 指定した耐久値より残っていれば合格！
                        if (remaining > ModConfig.MinAxeDurability) {
                            return i; // 使用可能な斧のスロット番号
                        }
                    } else {
                        // 耐久概念のない特殊な斧ならそのまま合格
                        return i;
                    }
                    // 合致しない場合は別の斧を探す
                }
            }
            if (IsLectern){
                // 書見台なら
                if (stack.is(Items.LECTERN)) {
                    // 見つかったスロットに切り替える
                    return i;
                }
            }
        }
        if (FoundAxe) return -2; //あるけどボッコボコ
        else return -1; // 見つからなかった
    }

    public static boolean TheoreticalCheck(int Lv, int Cost, boolean IsInverted, boolean IsTreasure){
        int Multiply = IsTreasure ? 2 : 1; //トレジャーエンチャントなら2倍の値段になる。
        List<Integer> TheoreticalList = List.of(5,8,11,14,17);
        if (IsInverted){
            TheoreticalList  = List.of(19,32,45,58,64);
            return TheoreticalList.get(Lv - 1) * Multiply <= Cost;
        }
        else {
            return TheoreticalList.get(Lv - 1) * Multiply == Cost;
        }
    }
}