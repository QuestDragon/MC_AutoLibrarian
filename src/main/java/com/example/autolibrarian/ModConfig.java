package com.example.autolibrarian; // 自分のパッケージ名に合わせる

import com.example.autolibrarian.AutoRerollHandler.AxeProtectionMode; // AxeProtectionModeのパッケージ
import com.example.autolibrarian.AutoRerollHandler.FoundAfterOperations; // AxeProtectionModeのパッケージ
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {

    // --- General Settings (基本設定) ---
    // ① ステータス表示のON/OFF
    public static boolean ShowStatus = true;

    // ② 理論値強調表示のON/OFF
    public static boolean UsingColoredCost = true;

    // ③ 斧の使用ON/OFF
    public static boolean UsingAxe = true;

    // ④ 斧の保護モード (ALL / ENCHANTED / OFF)
    public static AxeProtectionMode AxeProtect = AxeProtectionMode.ENCHANTED;

    // ⑤ 斧の最低耐久値
    public static int MinAxeDurability = 10;

    // ⑥ 目標エンチャント発見時の処理
    public static FoundAfterOperations AfterOperation = FoundAfterOperations.NOTHING;


    // --- Target Books Settings (欲しい本の設定) ---
    // ターゲット本データをまとめて管理するリスト
    public static List<TargetBook> TargetBooks = new ArrayList<>();

    // 1冊あたりのターゲット本データを表す内部クラス
    public static class TargetBook {
        public String enchantmentId; // 例: "minecraft:mending"
        public int level;            // 例: 1
        public int maxPrice;         // 例: 20

        public TargetBook(String enchantmentId, int level, int maxPrice) {
            this.enchantmentId = enchantmentId;
            this.level = level;
            this.maxPrice = maxPrice;
        }
    }

    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝保存処理＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝

    // 設定ファイルの保存先 (.minecraft/config/autolibrarian.json)
    private static final File CONFIG_FILE = new File(Minecraft.getInstance().gameDirectory, "config/autolibrarian.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 保存対象のデータ構造（JSON化しやすいようにまとめたデータクラス）
    public static class ConfigData {
        public List<TargetBook> targetBooks = new ArrayList<>();
        public boolean showStatus = true;
        public boolean usingColoredCost = true;
        public boolean usingAxe = true;
        public AxeProtectionMode axeProtectMode = AxeProtectionMode.OFF;
        public int axeProtectDurability = 10;
        public FoundAfterOperations afterOperationMode = FoundAfterOperations.NOTHING;
    }

    /**
     * 現在の設定値を JSON ファイルに保存する
     */
    public static void save() {
        try {
            // 親フォルダ（config ディレクトリ）が存在しない場合は作成
            if (!CONFIG_FILE.getParentFile().exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
            }

            ConfigData data = new ConfigData();
            data.showStatus = ShowStatus;
            data.usingColoredCost = UsingColoredCost;
            data.usingAxe = UsingAxe;
            data.axeProtectMode = AxeProtect;
            data.axeProtectDurability = MinAxeDurability;
            data.afterOperationMode = AfterOperation;
            data.targetBooks = TargetBooks;

            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 起動時に JSON ファイルから設定を読み込む
     */
    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save(); // ファイルがまだ無ければデフォルト値で新規作成
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null) {
                ShowStatus = data.showStatus ;
                UsingColoredCost = data.usingColoredCost;
                UsingAxe = data.usingAxe;
                AxeProtect = data.axeProtectMode;
                MinAxeDurability = data.axeProtectDurability;
                AfterOperation = data.afterOperationMode;
                if (data.targetBooks != null) {
                    TargetBooks = data.targetBooks;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}