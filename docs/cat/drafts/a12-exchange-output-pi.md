---
cat_version: '0.3'
kind: pi
id: simplecoins.currency.pi.exchange-result
status: draft
process: simplecoins.currency.crafting-exclusion
source_refs:
  - https://drive.google.com/file/d/1BscHPnPya9gFa36JQY4GBMcpX0TU_8p5/view
  - https://github.com/surumeneco/SimpleCoins/pull/444
decision_ref: ''
refs: []
semantic_contract: markdown-v2
language: ja
---

# PI: クラフト確定境界の観測・許否判断 (Draft)

## 境界
| 相手Process | 方向 |
| --- | --- |
| paper.crafting-lifecycle | 双方向 |

## 項目
| ID | 方向 | Domain | 存在 |
| --- | --- | --- | --- |
| 入力.貨幣目印あり | 受信 | 真偽 | 必須 |
| 入力.正規両替レシピ | 受信 | 真偽 | 必須 |
| 入力.正規投入額面 | 受信 | 真偽 | 必須 |
| 入力.登録済み出力一致 | 受信 | 真偽 | 必須 |
| 入力.現物結果一致 | 受信 | 真偽 | 必須 |
| 出力.許可 | 送信 | 真偽 | 必須 |
| 出力.準備結果を空にする | 送信 | 真偽 | 必須 |
| 出力.取消 | 送信 | 真偽 | 必須 |

## 操作
| ID | 方向 | 起点 |
| --- | --- | --- |
| prepare | 受付 | 準備判定 |
| craft | 受付 | 手動確定判定 |
| crafter | 受付 | 自動確定判定 |

## 制約
| 式 |
| --- |
| `入力.貨幣目印あり == 真 ならば 入力.正規投入額面 == 真 または 出力.許可 == 偽` |

## 各項目の意味
- `正規両替レシピ`: IDだけでなく、この操作時に存在するレシピがSimpleCoins規定20式の**入力・出力**に一致すること。
- `正規投入額面`: 使用される各スロットの貨幣PDCと固定モデル・額面・必要個数が式に一致し、不正なPAPER/混入物がないこと。
- `登録済み出力一致`: 現行レシピの結果ItemStackの貨幣識別子・固定モデル・枚数が式どおりであり、STONE等への差替えがないこと。
- `現物結果一致`: 実際にプレイヤー/クラフターが取得する結果の貨幣識別子・固定モデル・枚数が式どおりであること。
- `準備結果を空にする`と`取消`: 準備中は結果スロットを消し、手動/自動確定時はクラフトをキャンセルする。イベント後置換によって判定時と結果が食い違う場合の封鎖方法は未決。
