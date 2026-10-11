# YouTube Music 対応 — 統合メモ

ローカルの MediaStore ライブラリに加えて **YouTube Music** をオンライン音源として
追加する。アプリの UI/UX は変更しない。`:innertube` モジュールは
[OuterTune](https://github.com/OuterTune/OuterTune)（GPL-3.0）と同じ手法を実装しており、
非公式の **InnerTube** JSON API を叩き、**NewPipeExtractor** でストリームURLを署名解除する。

## 動作の流れ

```
検索 / ライブラリ  ->  YouTubeMusicRepository  ->  :innertube YouTube.search/browse
                                                    |
PlaybackConnection.playTracks(uri が "ytmusic:<videoId>" の Track)
                                                    |
PlaybackService (ExoPlayer)                         v
  ResolvingDataSource  ---- videoId ---->  OnlineStreamResolver
       |                                        |  クライアントを順に YouTube.player()
       |  実 https URL                           |  NewPipeUtils.getStreamUrl()（署名 + n）
       v                                        v
   OkHttpDataSource  --------------------------------->  音声バイト
```

- **InnerTube クライアント**（`innertube/.../InnerTube.kt`）は
  `https://music.youtube.com/youtubei/v1/{search,browse,player}` に POST する。
  偽装ヘッダ（`X-YouTube-Client-Name/Version`、`X-Origin`、Firefox UA）と
  `context.client` を付与し、Ktor + OkHttp で送る。
- **解析**（`YouTube.kt`、`Models.kt`）は `musicResponsiveListItemRenderer` を
  名前で集めて曲 / アルバム / プレイリスト / アーティストに分類する。型付き renderer
  ツリーは深く頻繁に変わるため、40以上のデータクラスを束縛せずこの方式にしている。
- **ストリームURL**（`NewPipe.kt`）は直接 `url` と `signatureCipher`（`s`/`sp`）、
  さらに `n` スロットリングパラメータの両方に対応し、NewPipe の
  `YoutubeJavaScriptPlayerManager` で解除する。
- **再生**はキュー/UI の変更が不要。オンライン曲は `ytmusic:<videoId>` の URI を
  持つだけで、`ResolvingDataSource` が取得直前に実URLへ差し替える。

## ファイル一覧

| パス | 役割 |
|---|---|
| `innertube/build.gradle.kts` | JVM モジュール（Kotlin 2.0.21 / JVM 17） |
| `innertube/src/.../YouTubeClient.kt` | クライアント定義（`ANDROID_VR_NO_AUTH`, `IOS`, …）と locale |
| `innertube/src/.../InnerTube.kt` | Ktor トランスポートと context / ヘッダ組み立て |
| `innertube/src/.../Models.kt` | ドメインモデルと防御的 JSON ヘルパー |
| `innertube/src/.../YouTube.kt` | search / album / playlist / artist / player |
| `innertube/src/.../NewPipe.kt` | 署名 + `n` パラメータのストリームURL解決 |
| `app/.../data/online/YouTubeMusicRepository.kt` | YT結果を `Track`/`OnlineCollection` にマッピング |
| `app/.../playback/OnlineStreamResolver.kt` | videoId → 再生可能URL（キャッシュ付き） |
| `app/.../playback/PlaybackService.kt` | OkHttp + `ResolvingDataSource` のwiring |
| `app/.../data/model/Track.kt` | `Track.online(...)`, `isOnline`, `videoId`, `thumbnailUrl` |
| `app/.../data/media/ArtworkLoader.kt` | ローカルは埋め込み画像、オンラインはサムネイルURLから取得 |
| `app/.../ui/search/SearchScreen.kt` | 検索に曲＋アルバム/プレイリスト/アーティストを表示、タップで再生 |
| `LICENSE` | GPL-3.0 全文 |

## 確認手順

```
./gradlew :innertube:compileKotlin     # モジュール単体のコンパイル
./gradlew testDebugUnitTest            # 既存ユニットテストが通ること
./gradlew assembleDebug                # モジュール込みでアプリがビルドできること
```

その後の実機確認: **検索** を開き2文字以上入力すると、ローカル結果の後に
YouTube Music の結果（アルバム名が「YouTube Music」）が並ぶ。それをタップすると
ストリーミング再生されるはず。

## 注意点

- **未コンパイル・未実行。** この環境にはシェルもコンパイラも無いため、上記コードは
  一切検証できていない。最初の実チェックは `:innertube:compileKotlin`。軽微な修正
  （import、NewPipe の API 面、クライアント一覧）は想定内。
- **ストリーム再生可否。** YouTube は一部クライアントにしか使える音声URLを返さない。
  `OnlineStreamResolver` は `ANDROID_VR_NO_AUTH → IOS → ANDROID → WEB_REMIX` の順に
  試す。一部の曲は本移植が生成していない **PO トークン** を必要とし、解決に失敗する
  ので、そうした曲は resolver 側で対応が必要。
- **バージョン。** Ktor は Kotlin 2.0.21 互換の `3.0.3` に固定。依存解決で Kotlin
  メタデータ非互換が出たら `gradle/libs.versions.toml` の `kotlin` と `ktor` を
  セットで上げること。
- **ライセンス。** OuterTune / NewPipeExtractor から移植しており両者とも **GPL-3.0**。
  このアプリは GPL-3.0 の派生著作物になるため `LICENSE`（GPL-3.0 全文）を同梱済み。
  配布時は帰属表示も残すこと。
- **地域。** YouTube Music は一部地域ではプロキシ/VPN 無しに使えない
  （そのために `YouTube.proxy` を公開している）。
