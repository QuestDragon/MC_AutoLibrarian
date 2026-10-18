AutoLibrarian - v1.1.1 (Created By QuestDragon)

～作成した経緯～
あるFabricのModには司書ガチャを自動で行ってくれる機能が入っていたんです。
Forgeにも独立したModがあるんじゃないかと探してみましたが、Fabricしかないという…なぜ？
ということなので自分で作りました。ほぼGeminiに頼ったんですけどね。

～機能～
キーボード操作1つで付近の村人と書見台をフックし、司書ガチャを行います。
対象の村人が司書へ就職、あるいは無職へ戻るまでを監視し、取引内容を自動で確認し、結果を通知します。
通知される結果にはエンチャント名、エンチャントのレベル、エメラルド価格が含まれており、エメラルド価格においては理論値かどうかも判定します。
設置可能なスペースを自動で探索し、その位置に書見台を自動的に設置、破壊してガチャを行います。
書見台破壊時、斧をホットバーに置いている場合はその斧を使用して素早く破壊します。
斧には保護機能が付いており、耐久値が少なくなると使用可能な別の斧を使用するか、斧を使用しなくなります。
設定内容に合致したエンチャント本が取引内容に含まれると、自動停止します。
設定内容に合致したエンチャント本が取引内容に含まれた段階で、設定内容を自動的に変更する機能も付いています。（指定価格を下げる、設定から取り除く、何もしない）
Modの表記はMinecraftの言語設定に基づいて表示されるようになっています。現状では英語と日本語に対応していますが、Jsonファイルを作成・編集することで別の言語での表記を追加したり、異なる表記へ変更することも可能です。

～機能追加、フィードバックについて～
制作者は初心者なので何かと至らないところがあると思います。
不具合等を発見しましたら、QuestDragonまでご連絡ください。
また、「こんな機能がほしい！」「ここはこうしてほしい！」という要望がありましたらご相談ください。
こちらもスクリプトModについて勉強したいので、ご意見や要望はいつでもお待ちしております。

～同梱物～
zipファイルには以下のファイルが含まれています。
AutoLibrarian-1.1.1+1.20.1-forge.jar：Mod本体ファイルです。
Readme.txt：説明書です。（本ファイル）

～インストール～
jarファイルをModsフォルダに配置して下さい。
Minecraftを起動するとロードされます。

～各種設定～
設定は専用の設定画面を開いて行います。キー設定についてはMinecraft内の操作設定をご確認ください。
設定画面は2ページで構成されており、「一般設定」と「ウィッシュリスト」に分かれています。
一般設定では、Modの動作を設定することが出来ます。以下に内容を記載します。なお、マウスオーバーすることでゲーム内でも説明を見ることが出来ます。
動作ステータスの表示 / Show status：Mod動作中、現在のModの状態を表示します。
理論値の強調表示 / Highlight of theoretical value：村人との取引価格が理論値の場合、チャットに通知されるエメラルド価格の文字色が専用色になります。最安値はマゼンタ、最高値は赤で表示されます。この設定を有効にするとjson側の装飾コードは無視されます。
書見台破壊に斧を使用 / Using axe：書見台破壊時、ホットバーに斧がある場合は自動的に使用します。
斧破壊保護 / Axe protection：Modが斧を使用する場合に、斧が破壊されないよう保護する設定です。全ての斧を対象にしたり、エンチャント済みの斧のみを対象にすることが出来ます。
斧破壊保護対象耐久値 / Axe protect durability：斧破壊保護設定が作動する耐久値を指定できます。耐久値がここで指定した値に達するまで、Modは斧を使用し続けます。
目標エンチャント本出現後の処理 / After operation：目的のエンチャント本が取引内容に表示された際の処理内容を設定できます。該当のエンチャント本を1エメラルド価格を下げて設定更新するか、該当のエンチャント本をリストから削除するかを選択できます。

ウィッシュリスト欄では、AutoLibrarianを自動停止させるエンチャント本を指定できます。
ここで指定したエンチャント本が取引に出現すると、AutoLibrarianは自動で動作を停止します。
After operationが設定されている場合は指定された処理も同時に行われます。
「追加… / Add new…」を選択するとエンチャント本一覧が表示されるので、目標とするエンチャントを選択し、下部でエンチャントレベルと許容する取引価格を指定して「追加 / Add」を選択してください。
指定したエンチャント名とレベル、かつ取引価格が指定した価格以下であればAutoLibrarianは動作を停止するようになります。
なお、レベルは完全一致しないと停止しないようになっています。つまり、耐久力1を指定した場合、取引内容に耐久力3が出てもAutoLibrarianは動作を停止させませんのでご注意ください。
既に追加したエンチャント本のレベルや許容する取引価格を変更する場合は、該当のエンチャント本をリストから選択し、下部のプラス、マイナスボタンで行うことが出来ます。
「削除 / Remove」ボタンを選択すると選択したエンチャント本をリストから削除することが出来ます。この操作は取り消すことが出来ませんので十分ご注意ください。

～使い方 <本体編>～
ホットバーに書見台を含めておきます。1つで大丈夫です。
必要に応じて斧をご準備下さい。無くても動きますが、あると作業が早くなります。
最初に書見台を置きたい場所をスニークしながら右クリックして座標指定しておきます。正常に指定できるとメッセージが表示されます。
Minecraft内の操作設定で「動作の切替」に指定したキーを押すと有効になります。もう一度押すと無効になります。
設定内容に合致したエンチャント本が取引内容に含まれると自動的に停止します。

～使い方 <json編>～
Minecraftの言語設定に応じて本Modが表記する内容を編集することが出来ます。
日本語の場合はja_jp.jsonをご使用ください。
以下にキーと使われている場所について記述しておきます。
    "category.autolibrarian": 操作設定内のカテゴリ名
    "key.autolibrarian.toggle": 操作設定内のMod動作切り替え用キー設定名
    "key.autolibrarian.menu": 操作設定内のMod設定メニュー表示用キー設定名

    "general.autolibrarian.on": Modの至る所で使われる、オンの時の文字
    "general.autolibrarian.off": Modの至る所で使われる、オフの時の文字
    "general.autolibrarian.add": Modの至る所（現状は本設定画面）で使われる、追加ボタンの文字
    "general.autolibrarian.add_new": Modの至る所（現状は本設定画面）で使われる、追加画面に遷移するボタンの文字
    "general.autolibrarian.remove": Modの至る所（現状は本設定画面）で使われる、削除ボタンの文字
    "general.autolibrarian.back": Modの至る所（現状は本設定画面）で使われる、戻るボタンの文字

    "gui.autolibrarian.config.title": Modの設定画面上部に表示されるタイトル
    "gui.autolibrarian.config.general": Modの一般設定を表示するためのボタンの文字
    "gui.autolibrarian.config.target_book": Modのエンチャント本指定設定画面を表示するためのボタンの文字
    "gui.autolibrarian.config.show_status": Modの一般設定画面における、Mod動作ステータスを表示させる設定項目の表示文字
    "gui.autolibrarian.config.using_colored_cost": Modの一般設定画面における、Mod動作時に取引内容を通知するチャットの価格部分を専用色にする設定項目の表示文字
    "gui.autolibrarian.config.using_axe": Modの一般設定画面における、書見台破壊に斧を使用する設定項目の表示文字
    "gui.autolibrarian.config.axe_protection": Modの一般設定画面における、斧の使用時に耐久値に基づいて斧を保護する設定項目の表示文字
    "gui.autolibrarian.config.axe_protection_value": Modの一般設定画面における、斧の許容耐久値を設定する項目の表示文字
    "gui.autolibrarian.config.axe_protection_all": Modの一般設定画面内、斧破壊保護設定における、斧の破損防止機能の動作対象を全ての斧に設定する場合の設定名
    "gui.autolibrarian.config.axe_protection_enchanted": Modの一般設定画面内、斧破壊保護設定における、斧の破損防止機能の動作対象をエンチャント済み斧にのみ設定する場合の設定名
    "gui.autolibrarian.config.foundafter": Modの一般設定画面における、目的のエンチャントが取引画面に出現した際に行われる処理の設定項目の表示文字
    "gui.autolibrarian.config.foundafter_price": Modの一般設定画面内、目的エンチャント発見後処理における、価格を今回の結果より1下げて更新する場合の設定名
    "gui.autolibrarian.config.foundafter_remove": Modの一般設定画面内、目的エンチャント発見後処理における、該当エンチャント本を削除する場合の設定名

    "gui.autolibrarian.config_tips.show_status": Modの一般設定画面における、Mod動作ステータスを表示させる設定項目のツールチップ内容
    "gui.autolibrarian.config_tips.using_color_cost": Modの一般設定画面における、Mod動作時に取引内容を通知するチャットの価格部分を専用色にする設定項目のツールチップ内容
    "gui.autolibrarian.config_tips.using_axe": Modの一般設定画面における、書見台破壊に斧を使用する設定項目のツールチップ内容
    "gui.autolibrarian.config_tips.axe_protection": Modの一般設定画面における、斧の使用時に耐久値に基づいて斧を保護する設定項目のツールチップ内容
    "gui.autolibrarian.config_tips.axe_protection_value": Modの一般設定画面における、斧の許容耐久値を設定する項目のツールチップ内容
    "gui.autolibrarian.config_tips.foundafter": Modの一般設定画面における、目的のエンチャントが取引画面に出現した際に行われる処理の設定項目のツールチップ内容

    "gui.autolibrarian.target_books.enchant_level": Modのエンチャント本設定画面における、目的のエンチャントのレベルを指定する欄の表示文字
    "gui.autolibrarian.target_books.sell_price": Modのエンチャント本設定画面における、目的のエンチャントの許容取引価格を指定する欄の表示文字

    "status.autolibrarian.destroy": Mod動作時、書見台破壊開始時に表示されるステータス内容
    "status.autolibrarian.wait_destroy": Mod動作時、書見台破壊中に表示されるステータス内容
    "status.autolibrarian.wait_unemployed": Mod動作時、村人が無職に戻るまで待機している際に表示されるステータス内容
    "status.autolibrarian.place": Mod動作時、書見台設置中に表示されるステータス内容
    "status.autolibrarian.wait_librarian": Mod動作時、村人が司書に就職するまで待機している際に表示されるステータス内容
    "status.autolibrarian.talk": Mod動作時、村人の取引画面を表示しようとしている際に表示されるステータス内容
    "status.autolibrarian.check_trade": Mod動作時、村人の取引内容を確認している際に表示されるステータス内容

	"message.autolibrarian.set_pos": 書見台の座標指定を正常に行った場合のメッセージ文（%1にX座標、%2にY座標、%3にZ座標が入ります）
	"message.autolibrarian.cannot_set_pos": 書見台の座標指定時、指定できない座標に指定しようとした場合のメッセージ文
	"message.autolibrarian.out_of_pos": Mod動作開始時、書見台の座標指定をしていないか、指定した座標から離れすぎている場合のメッセージ文
    "message.autolibrarian.no_target_book": Mod動作開始時、目的のエンチャント本を1つも指定していない場合のメッセージ文
    "message.autolibrarian.not_enough_space": Mod動作開始時、書見台を設置するスペースがない場合のメッセージ文
    "message.autolibrarian.no_lectern": Mod動作開始時、書見台をホットバーに入れてない場合のメッセージ文
    "message.autolibrarian.axes_tip": Mod動作開始時、斧をホットバーに入れてない場合のメッセージ文
    "message.autolibrarian.whynotuse_myaxes_tip": Mod動作開始時、耐久値が少ししかない斧をホットバーに入れている場合のメッセージ文
    "message.autolibrarian.missing_lectern": Mod動作開始時、書見台をホットバーに入れてない場合のメッセージ文
    "message.autolibrarian.trade_detail": Mod動作時、取引内容にエンチャント本が含まれていた場合の販売内容
    （%1$sにはエンチャント名とレベル、%2$sにはエメラルド価格が入ります。エメラルド価格は自動で文字色が変わるので、独自の文字色は現在のところ設定できません。）
    "message.autolibrarian.trade_nobook": Mod動作に、取引内容にエンチャント本が含まれていなかった場合のメッセージ文

～余談～
Geminiにお世話になりました。途中でサーバー障害か何かで使えなくなりましたけど…。
まあ、便利な世の中になりましたね。
ちなみに私はC#はやったことありますが、Javaはないです。似たようなモノだと聞いてはいましたが、JavaはJavaなりの書き方があってそこには戸惑いましたね…。
あとはこういうMod系あるあるなんですが、そのゲーム（Modエンジン）独自のメソッドとかコードの書き方があるので、プログラム言語の構文学ぶよりそっちのほうが難しかったり…。
また、このModの設定画面ですが、当初はコマンドで実装する予定でした。でも使い勝手を考えるとUIの方が良い、とGeminiに言われたのでUIを作りました。結構大変だった。

～免責事項～
本Modを使用したことにより生じた被害に関して、私QuestDragonは一切の責任を負いかねます。自己責任でご使用ください。
2次配布は禁止です。
予告なく配布を停止することがあります。予めご了承ください。

制作：QuestDragon