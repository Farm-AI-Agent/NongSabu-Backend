from __future__ import annotations

import argparse
import json
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import requests
from pypdf import PdfReader


@dataclass(frozen=True)
class EvaluationCase:
    query_id: str
    query: str
    gt_answer: str
    source_pages: tuple[int, ...]
    required_terms: tuple[str, ...]


CASES = (
    EvaluationCase(
        query_id="grape_green_manure_001",
        query="포도 유기재배에서 녹비작물은 어떤 효과가 있고 토양에 환원할 때 무엇을 주의해야 하나요?",
        gt_answer=(
            "녹비작물은 토양의 입단화, 통기성, 보수력을 개선하고 유기물 분해를 통해 "
            "양분 공급과 토양의 양분 보유력 및 완충력을 높인다. 또한 염류집적, 토양유실, "
            "잡초와 일부 토양 병원균을 줄이는 데 도움이 된다. 환원 후 무기화에는 따뜻한 "
            "지역에서 약 2주, 저온 지역에서 약 4주가 필요하다. C/N율이 높은 화본과 녹비는 "
            "잘게 잘라 갈아엎고, 습해에 약한 콩과 녹비는 배수를 철저히 해야 한다."
        ),
        source_pages=(29, 30, 31),
        required_terms=("녹비작물", "입단", "무기화"),
    ),
    EvaluationCase(
        query_id="grape_magnesium_002",
        query="포도 잎맥 사이가 누렇게 변하고 장마 뒤 증상이 심해졌습니다. 원인과 대책은 무엇인가요?",
        gt_answer=(
            "마그네슘 결핍이 의심된다. 새가지 기부의 4~5번째 잎에서 잎맥 사이가 황색 또는 "
            "황백색으로 변하고 심하면 낙엽하며, 6월 하순부터 나타나 장마 뒤 7~8월에 심해진다. "
            "과습이나 건조, 강산성 토양, 칼리 과다가 원인이 될 수 있다. 토양산도를 pH "
            "6.0~6.5로 교정하고 마그네슘 함유 허용 유기자재를 사용하며 칼리 과다 시용을 피한다."
        ),
        source_pages=(36, 37),
        required_terms=("마그네슘", "장마", "6.0~6.5"),
    ),
    EvaluationCase(
        query_id="grape_flower_shatter_003",
        query="거봉에서 꽃이 핀 뒤 포도알이 드문드문 달립니다. 꽃떨이 원인과 예방 방법은 무엇인가요?",
        gt_answer=(
            "거봉의 꽃떨이는 개화기의 불량한 날씨, 저장양분 부족, 새가지의 과도한 생장, "
            "붕소 결핍, 강한 겨울전정 등으로 수정과 착과가 불량해져 발생할 수 있다. 조기낙엽, "
            "질소 과다, 과다결실을 피하고 강전정과 밀식을 줄여 수세를 안정시킨다. 붕사는 "
            "2년마다 10a당 2~3kg을 토양에 주거나 개화 1~2주 전에 0.3%액을 엽면 살포할 수 "
            "있으며, 세력이 강한 새가지는 개화 4~5일 전에 선단을 순지르기한다."
        ),
        source_pages=(40, 41),
        required_terms=("꽃떨이", "붕사", "순지르기"),
    ),
    EvaluationCase(
        query_id="grape_fruit_cracking_004",
        query="포도 성숙기에 열매 껍질이 갈라집니다. 열과를 줄이려면 어떻게 해야 하나요?",
        gt_answer=(
            "성숙기의 잦은 비로 과실이 갑자기 물을 많이 흡수하면 내부 압력이 높아져 열과가 "
            "발생한다. 밀착된 송이, 질소 과다, 강한 수세, 강전정과 밀식에 따른 일조 부족, "
            "배수 불량과 큰 토양수분 변동도 열과를 늘린다. 비가림이나 시설재배로 강우를 막고, "
            "멀칭과 건조기 관수로 토양수분의 급격한 변화를 줄이며, 배수와 수관의 채광·통풍을 "
            "개선하고 질소 과다 시용을 피한다."
        ),
        source_pages=(42, 43),
        required_terms=("열과", "토양수분", "비가림"),
    ),
    EvaluationCase(
        query_id="grape_downy_mildew_005",
        query="포도 잎에 담황록색 반점이 생기고 며칠 뒤 잎 뒷면에 흰 곰팡이가 보입니다. 어떤 병이며 어떻게 관리하나요?",
        gt_answer=(
            "포도 노균병의 전형적인 증상이다. 초기에는 잎에 경계가 불분명한 담황록색 반점이 "
            "생기고 4~5일 뒤 잎 뒷면에 흰 곰팡이가 나타난다. 병든 낙엽을 모아 땅속에 묻거나 "
            "태우고 질소 과다 시용을 피하며 통풍을 관리한다. 발아 전 석회유황합제를 살포하고, "
            "감수성 품종은 발병 전에 예방적으로 방제한다. 병원균이 주로 잎 뒷면의 기공으로 "
            "침입하므로 약액이 잎 뒷면까지 충분히 묻게 살포한다."
        ),
        source_pages=(47, 48),
        required_terms=("노균병", "담황록색", "잎 뒷면"),
    ),
    EvaluationCase(
        query_id="grape_orchard_site_006",
        query="포도원 개원 전 토양 만들기에서 권장하는 토양 조건과 유공관을 이용한 암거배수 설치 기준은 무엇인가요?",
        gt_answer=(
            "포도원은 토심이 깊고 비옥하며 건습 변화가 적고 물빠짐이 좋은 사질양토가 "
            "적합하다. 뿌리가 50cm 이상 깊게 자랄 수 있고 지하수위가 낮은 곳이 좋으며, "
            "논이나 물이 고이는 곳은 배수시설을 철저히 해야 한다. 외부 빗물 유입을 막도록 "
            "과수원 둘레에 도랑을 만들고 내부에는 약 1m 깊이로 유공관을 묻어 도랑에 "
            "연결한다. 암거배수관 간격은 일반적으로 약 5m로 하고 최대 10m를 넘기지 않으며, "
            "퇴적물로 막히지 않도록 적절한 구배를 확보한다."
        ),
        source_pages=(13, 14, 15),
        required_terms=("사질양토", "유공관", "10m"),
    ),
    EvaluationCase(
        query_id="grape_variety_selection_007",
        query="유기 포도재배용 품종을 선택할 때 어떤 기준을 고려해야 하나요?",
        gt_answer=(
            "재배지의 기후와 토양에 맞고 소비자 기호성이 높으며 재배 안정성이 좋은 품종을 "
            "선택해야 한다. 재배양식과 재배자의 기술 수준, 수송성과 상품성, 이용 목적도 "
            "함께 고려한다. 유기재배에서는 한 품종에 병해충이나 생리장해가 집중되는 위험을 "
            "줄이기 위해 2~3품종을 심는 것이 바람직하다. 품종별로 갈색무늬병, 깍지벌레, "
            "열과 등 취약점이 다르므로 해당 특성에 맞는 관리 가능 여부도 확인한다."
        ),
        source_pages=(19, 24, 25),
        required_terms=("품종 선택시 유의점", "2~3품종", "갈반병"),
    ),
    EvaluationCase(
        query_id="grape_soil_organic_matter_008",
        query="포도밭 토양 유기물은 어떤 역할을 하며 C/N율에 따라 무엇이 달라지나요?",
        gt_answer=(
            "토양 유기물은 분해되면서 다량·미량 원소를 공급하고, 불용화된 인산을 "
            "가용화하며, 양이온치환용량과 보비력을 높인다. 유용 미생물 증식과 떼알구조 "
            "형성을 돕고 보수성과 통기성을 개선해 뿌리 활력을 높인다. C/N율이 높으면 "
            "미생물과 작물 사이의 질소 경쟁으로 질소결핍이 생길 수 있고, C/N율이 낮으면 "
            "질소 무기화가 빨라져 양분 공급 효과가 커진다. 녹비와 양질의 퇴구비를 적절히 "
            "사용하고 작물 잔사를 토양에 환원하며 과도한 경운은 피한다."
        ),
        source_pages=(28, 29),
        required_terms=("인산 가용화", "C/N율", "질소경합"),
    ),
    EvaluationCase(
        query_id="grape_potassium_009",
        query="8월부터 포도 잎 가장자리의 잎맥 사이가 누렇게 되고 마르기 시작합니다. 어떤 결핍이며 어떻게 관리해야 하나요?",
        gt_answer=(
            "과실비대 중·후기인 8월 상순부터 잎 가장자리의 잎맥 사이가 황화되고, 심하면 "
            "괴사와 엽소가 나타나는 것은 칼리 결핍 증상이다. 토양 칼리 부족뿐 아니라 "
            "마그네슘 함량이 높은 경우의 길항작용, 질소 과다 시비, 과다 착과로 잎의 칼리 "
            "함량이 낮아져 발생할 수 있다. 토양과 잎의 양분 상태 및 Mg/K 균형을 확인하고, "
            "질소 과다 시비와 과다 착과를 피하면서 부족한 칼리를 적정하게 공급한다."
        ),
        source_pages=(37,),
        required_terms=("칼리 결핍", "8월 상순", "Mg/K"),
    ),
    EvaluationCase(
        query_id="grape_boron_010",
        query="포도 어린잎이 작고 기형이며 잎맥 사이에 작은 반점이 보입니다. 원인과 대책은 무엇인가요?",
        gt_answer=(
            "붕소 결핍이 의심된다. 경미할 때는 잎맥 사이에 유침상의 작은 반점이 생기고, "
            "생육이 왕성한 선단과 어린잎에서 증상이 심하며 잎이 작아지고 기형이 된다. "
            "뿌리 분포가 얕거나 문우병 등으로 뿌리가 손상되면 붕소 흡수가 저해될 수 있다. "
            "토양 시비나 엽면 살포로 붕소를 공급하고, 결핍이 쉬운 토양에서는 유기물을 "
            "충분히 넣어 완충능력과 뿌리 활력을 유지한다. 붕소는 미량요소이므로 과다 시용을 "
            "피하고 적정량을 사용해야 한다."
        ),
        source_pages=(38,),
        required_terms=("붕소 결핍", "유침상", "엽면살포"),
    ),
    EvaluationCase(
        query_id="grape_dormancy_disorder_011",
        query="재식 후 2~3년 된 포도나무에서 눈이 고르게 트지 않는 휴면병이 생겼습니다. 발생 원인과 예방법은 무엇인가요?",
        gt_answer=(
            "휴면병은 발아가 불량하거나 불균일하고 새가지가 잘 자라지 않으며, 심하면 가지가 "
            "갈라져 지상부가 고사하는 장해로 재식 후 2~3년 된 어린 나무에서 잘 발생한다. "
            "질소 과다와 밀식에 따른 늦자람, 저장 탄수화물 감소, 내한성이 약한 품종, 2월 "
            "이후 영하 10도 이하의 저온이 주요 원인이다. 적정 착과량을 유지해 수확을 "
            "지연시키지 말고 8월 상순에도 새가지가 자라면 순자르기한다. 수확 후에도 병해충 "
            "방제와 주기적인 관수를 계속해 저장양분과 내한성을 유지한다."
        ),
        source_pages=(39,),
        required_terms=("휴면병", "3년병", "–10℃"),
    ),
    EvaluationCase(
        query_id="grape_stem_necrosis_012",
        query="포도 성숙기에 송이자루가 갈색으로 마르고 포도알이 쉽게 떨어집니다. 원인과 방지 방법은 무엇인가요?",
        gt_answer=(
            "송이자루와 송이축이 갈색으로 말라 포도알이 떨어지는 것은 꼭지마름 증상이다. "
            "송이를 너무 많이 달거나 칼리 결핍으로 송이가 약해진 뒤 병원균이 침입하는 것이 "
            "주요 원인으로 알려져 있으며 유럽계 포도에서 비교적 많이 발생한다. 착과량과 "
            "양분 균형을 적절히 관리하고, 열매자루 괴사와 관련된 마그네슘 결핍을 줄이기 "
            "위해 개화기 이후 또는 8월 상순에 황산마그네슘 2~5%액을 엽면 살포하면 발생을 "
            "줄일 수 있다."
        ),
        source_pages=(43,),
        required_terms=("꼭지마름", "황산마그네슘", "2~5%"),
    ),
    EvaluationCase(
        query_id="grape_leaf_spot_013",
        query="캠벨얼리 잎의 흑갈색 점무늬가 서로 합쳐지고 8~9월에 조기 낙엽이 심해집니다. 갈색무늬병의 방제 방법은 무엇인가요?",
        gt_answer=(
            "갈색무늬병 또는 갈반병이 의심된다. 잎에 흑갈색 점무늬가 생긴 뒤 병반이 합쳐져 "
            "잎마름과 조기낙엽을 일으키며, 과실 당도와 저장양분을 떨어뜨린다. 장마가 길고 "
            "비가 잦거나 밀식한 과원에서 심하며 7월부터 발생해 8~9월에 많아진다. 수관의 "
            "채광과 통풍을 개선하고 배수와 질소 시비를 적절히 관리하며 감염 낙엽을 제거한다. "
            "발아 전에 석회유황합제를 살포하고 6월 상순부터 석회보르도액으로 방제하며 "
            "장마철 방제시기를 놓치지 않는다."
        ),
        source_pages=(46, 47),
        required_terms=("갈색무늬병", "8~9월", "석회보르도액"),
    ),
    EvaluationCase(
        query_id="grape_ripe_rot_014",
        query="성숙기 포도알에 적갈색의 둥근 병반과 연분홍색 포자 덩어리가 생깁니다. 어떤 병이며 관리 방법은 무엇인가요?",
        gt_answer=(
            "포도 탄저병 증상이다. 어린 과실에 담갈색 또는 흑갈색 작은 반점이 생기고 "
            "병반이 커지면서 연분홍 포자를 가진 적갈색 병반이 나타난다. 병원균은 과경, "
            "말라붙은 병든 포도알과 가지에서 월동하며 6월 중·하순부터 감염되어 착색기와 "
            "성숙기에 비가 많으면 피해가 커진다. 비가림시설을 설치하고 늦어도 6월 말 "
            "콩알 크기 때까지 봉지를 씌운다. 겨울전정과 생육 중 병든 조직을 제거하고, 발아 "
            "전 석회유황합제를 살포하며 생육기에는 강우 상황에 맞춰 주기적으로 방제한다."
        ),
        source_pages=(48, 49),
        required_terms=("탄저병", "연분홍", "6월말"),
    ),
    EvaluationCase(
        query_id="grape_powdery_mildew_015",
        query="포도 잎과 어린 과실 표면에 회백색 곰팡이가 생겼습니다. 흰가루병의 발생 조건과 관리법은 무엇인가요?",
        gt_answer=(
            "흰가루병은 잎에 원형의 황록색 반점과 담백색 포자 덩이를 만들고, 가지와 "
            "과립에는 회백색 곰팡이를 형성한다. 병든 부위나 눈의 인편에서 균사로 월동하며 "
            "24~30도에서 활동이 왕성하고 이른 봄부터 초여름까지 고온·건조할 때 발생이 "
            "많다. 5월부터 10월까지 나타나며 6월 하순부터 7월 상순에 가장 심하다. 수관의 "
            "통풍과 채광을 개선하고 병든 가지와 낙엽을 제거한다. 월동 후 석회유황합제를 "
            "나무 전체에 살포하고 과립이 밀착되기 전에 보르도액을 살포한다."
        ),
        source_pages=(49, 50),
        required_terms=("흰가루병", "24~30℃", "6월 하순"),
    ),
    EvaluationCase(
        query_id="grape_shine_muscat_browning_016",
        query="샤인머스켓 과실 표면에 갈색 반점이 생기는 과피갈변증상은 왜 발생하며 봉지는 언제 제거하는 것이 좋나요?",
        gt_answer=(
            "샤인머스켓의 과피갈변은 물리적 손상이 아니라 표피 밑 세포에서 발생하는 "
            "생리장해이다. 당도가 17°Bx 이상으로 높아지고 수확기가 가까워질수록 발달하며, "
            "질소가 많고 칼슘이 적은 토양에서 많이 발생하므로 질소질비료 사용을 줄여야 한다. "
            "봉지를 수확 2~3주 전에 제거하면 숙기가 빨라지고 과피가 황색에 가까워지지만 "
            "과피갈변과 탄저병이 발생할 수 있다. 안정적인 생산을 위해서는 수확 1주 전이나 "
            "수확일에 봉지를 제거하는 것이 좋다."
        ),
        source_pages=(22,),
        required_terms=("과피갈변증상", "17°Bx", "수확 1주전"),
    ),
    EvaluationCase(
        query_id="grape_bunch_shriveling_017",
        query="시설재배 포도 일부 알에 흑갈색 점무늬가 커지면서 움푹 들어가는 축과 증상이 나타나는 원인과 예방법은 무엇인가요?",
        gt_answer=(
            "축과는 주로 시설재배에서 발생하며 처음에는 과피에 흑갈색 점무늬가 생기고 "
            "점차 커져 해당 부위가 움푹 들어간다. 질소질거름 과다나 지나친 토양수분으로 잎이 "
            "왕성하게 자라면 포도송이가 잎에 수분을 빼앗기고, 뿌리의 수분 흡수량과 잎의 "
            "증산량 사이에 불균형이 생겨 발생이 많아진다. 급격한 지하수위 상승, 병해충에 "
            "의한 뿌리 손상, 질소 과용, 강전정 등 지상부 증산과 지하부 흡수의 불균형을 "
            "일으키는 조건을 피해야 한다."
        ),
        source_pages=(44,),
        required_terms=("축과", "흑갈색 점무늬", "증산량"),
    ),
    EvaluationCase(
        query_id="grape_gray_mold_018",
        query="개화기 전후 포도 꽃과 신초가 갈색으로 마르고 성숙한 과실이 썩는 잿빛곰팡이병은 어떤 조건에서 많아지며 어떻게 관리하나요?",
        gt_answer=(
            "잿빛곰팡이병은 캠벨얼리와 거봉 등에서 개화기 전후 습도가 높을 때 많이 "
            "발생한다. 꽃과 신초가 감염되면 갈색으로 변해 마르고, 잎에는 크고 부정형인 "
            "검붉은 반점이 생기며, 성숙한 과실은 상처나 약한 과피를 통해 감염된다. "
            "병원균은 노화하거나 죽은 조직, 휴면아와 표피 속에서 균핵과 균사로 월동한다. "
            "밀식, 강전정, 질소 과용을 피하고 수관 내부의 채광과 통풍을 개선하며, 시설에서는 "
            "환기와 배수 관리를 철저히 해야 한다."
        ),
        source_pages=(50, 51),
        required_terms=("잿빛곰팡이병", "균핵", "환기"),
    ),
    EvaluationCase(
        query_id="grape_birds_eye_rot_019",
        query="포도 어린잎 병반에 구멍이 뚫리고 과립에 새눈 모양 반점이 생기는 병의 발생 특성과 관리법은 무엇인가요?",
        gt_answer=(
            "새눈무늬병 또는 흑두병이다. 봄철 비가 자주 올 때 경화되기 전의 어린잎, 줄기, "
            "덩굴손과 과실에 발생한다. 잎에는 가장자리가 적갈색인 작은 반점이 생겨 서로 "
            "합쳐진 뒤 구멍이 뚫리고, 신초에는 타원형 흑갈색 병반이 생기며, 7~8월에는 "
            "과립에 새눈 모양 반점이 나타난다. 병든 가지에서 균사로 월동하고 12℃ 이상에서 "
            "비가 오면 포자가 형성되어 빗물로 전염된다. 비가림재배를 하고 병든 조직을 "
            "제거하며, 신초가 약 5cm 자란 때부터 장마철까지 중점적으로 방제한다."
        ),
        source_pages=(51, 52),
        required_terms=("새눈무늬병", "12℃", "신초가 5cm"),
    ),
    EvaluationCase(
        query_id="grape_mirid_bug_020",
        query="포도 어린잎에 바늘로 찌른 듯한 갈색 반점과 구멍이 생기고 잎이 기형이 됩니다. 어떤 해충 피해이며 방제 시기는 언제인가요?",
        gt_answer=(
            "애무늬고리장님노린재 피해가 의심된다. 이 해충은 포도 눈의 인편 틈에서 알로 "
            "월동하고 신초가 약 3cm인 3~4엽기에 부화한다. 약충과 성충이 어린잎과 과실의 "
            "즙액을 빨아 피해 부위가 갈색으로 죽고, 이후 구멍이 생기며 잎이 위축되고 "
            "기형화된다. 개화 전후에 피해를 받으면 꽃송이 고사, 과피 흑변과 소립과가 생길 "
            "수 있다. 발아기부터 꽃송이 형성기까지 예찰·방제하고 개화 15~20일 전까지 "
            "방제를 마무리한다."
        ),
        source_pages=(52, 53),
        required_terms=("애무늬고리장님노린재", "3~4엽기", "15~20일전"),
    ),
    EvaluationCase(
        query_id="grape_leafhopper_021",
        query="포도 잎 뒷면을 해충이 빨아먹어 잎 표면이 회백색으로 변하고 그을음 피해가 생깁니다. 애매미충의 발생 시기와 방제 적기는 언제인가요?",
        gt_answer=(
            "애매미충류는 성충으로 낙엽, 잡초 밑과 거친 껍질 틈에서 월동하며 1년에 3회 "
            "발생한다. 1회 성충은 6월 중순~7월 상순, 2회는 8월 중·하순, 3회는 9월 "
            "하순~10월 상순에 나타난다. 약충과 성충이 주로 잎 뒷면에서 즙액을 빨아 작은 "
            "반점과 회백색 변색을 일으키고, 배설물은 광합성을 억제하며 과실에 그을음병을 "
            "유발할 수 있다. 월동 성충이 활동하는 발아기인 4월 중순부터 다음 세대 성충이 "
            "나타나는 6월 중·하순 이전까지가 방제 적기이다."
        ),
        source_pages=(53, 54),
        required_terms=("애매미충", "1년에 3회", "4월 중순"),
    ),
    EvaluationCase(
        query_id="grape_mealybug_022",
        query="포도 가지에 흰 밀랍이 묻고 잎과 열매에 그을음이나 기형이 생깁니다. 가루깍지벌레는 언제 어떻게 방제해야 하나요?",
        gt_answer=(
            "가루깍지벌레는 흰 밀랍으로 덮인 해충으로 나무껍질 밑이나 틈에서 주로 알로 "
            "월동하며 약충이나 성충으로도 월동한다. 1년에 약 3회 발생하고 4월 하순~5월 "
            "상순에 부화한 어린 약충은 줄기 밑이나 잎에서 지내다가 과실로 이동한다. 수액을 "
            "빨아 수세를 약화시키고 그을음병과 과실 기형을 일으킨다. 월동처인 줄기의 거친 "
            "껍질을 제거하고 발견 즉시 방제한다. 성충은 밀랍 때문에 약액이 잘 묻지 않으므로 "
            "밀랍을 덮기 전 약충기에 방제하는 것이 효과적이다."
        ),
        source_pages=(54, 55),
        required_terms=("가루깍지벌레", "4월 하순", "약충"),
    ),
    EvaluationCase(
        query_id="grape_spotted_lanternfly_023",
        query="포도나무 줄기에 꽃매미가 모여 수액을 빨고 검은 그을음이 생깁니다. 발생 생태와 방제 방법은 무엇인가요?",
        gt_answer=(
            "꽃매미는 국내에서 알로 월동하며 1년에 1회 발생한다. 남부지방에서는 4월 "
            "하순부터 알이 부화하고 7월부터 성충이 나타나며, 9월 하순부터 교미한 뒤 월동할 "
            "알을 낳는다. 약충과 성충이 줄기의 즙액을 빨아 수세를 떨어뜨리고 감로를 분비해 "
            "검은 그을음 증상을 일으키며, 피해가 심하면 작은 가지가 시들거나 과실 상품성이 "
            "낮아진다. 전정할 때 나무에 붙은 알덩어리를 제거하고 난괴 표면에 10~20배액의 "
            "기계유유제를 살포하며, 약 5×6mm 방충망을 설치할 수 있다."
        ),
        source_pages=(55, 56),
        required_terms=("꽃매미", "10~20배액", "5×6mm"),
    ),
    EvaluationCase(
        query_id="grape_tiger_longhorn_024",
        query="포도 결과모지 윗부분이 마르고 가지 속이 비어 쉽게 부러집니다. 포도호랑하늘소의 생활사와 방제법은 무엇인가요?",
        gt_answer=(
            "포도호랑하늘소 유충 피해가 의심된다. 어린 유충은 포도 가지 속에서 월동한 뒤 "
            "4월 초부터 줄기 내부를 먹으며, 줄기 안에서 번데기가 된다. 성충은 7월 하순부터 "
            "9월 중순까지 발생해 줄기 마디의 눈과 잎자루 틈에 알을 낳는다. 유충이 "
            "결과모지의 눈 부분으로 들어가 목질부를 가해하면 윗부분이 말라죽고 가지 속이 "
            "비어 쉽게 부러지며 내부가 갈색으로 변한다. 이른 봄 전정할 때 표피가 검게 변한 "
            "피해 가지를 찾아 한곳에 모아 태운다."
        ),
        source_pages=(56, 57),
        required_terms=("포도호랑하늘소", "7월 하순", "불에 태운다"),
    ),
    EvaluationCase(
        query_id="grape_spider_mite_025",
        query="시설 포도 잎 뒷면에 흰 반점이 생기고 심하면 잎이 적갈색으로 변합니다. 응애류의 발생 특성과 관리법은 무엇인가요?",
        gt_answer=(
            "포도에 발생하는 주요 응애류는 점박이응애와 차응애이며 노지보다 시설재배에서 "
            "문제가 크다. 점박이응애는 1년에 8~9세대 발생하고 나무껍질 밑, 잡초와 낙엽 등 "
            "지표를 덮은 물체에서 성충으로 월동한다. 주로 잎 뒷면에서 흡즙해 엽록소를 "
            "파괴하므로 흰 반점이 생기고 피해가 심하면 잎이 적갈색으로 변한다. 포도원 "
            "주변의 잡초를 제거해 은신처를 없애고, 번식 속도가 빠르므로 발생 초기에 "
            "방제해야 한다."
        ),
        source_pages=(57,),
        required_terms=("점박이응애", "8~9세대", "잡초를 제거"),
    ),
    EvaluationCase(
        query_id="grape_soil_chemistry_026",
        query="포도 과원의 적정 토양 pH와 유기물, 유효인산, EC 기준은 어떻게 되나요?",
        gt_answer=(
            "포도 과원의 적정 토양 화학성은 pH 6.0~6.5, 유기물 20~35g/kg, 유효인산 "
            "200~300mg/kg이며 전기전도도인 EC는 2.0dS/m 이하이다. 치환성 양이온은 "
            "칼륨 0.3~0.6cmol+/kg, 칼슘 5.0~6.0cmol+/kg, 마그네슘 "
            "1.2~2.0cmol+/kg가 적정 범위로 제시된다."
        ),
        source_pages=(27,),
        required_terms=("20∼35", "200∼300", "2.0이하"),
    ),
    EvaluationCase(
        query_id="grape_disease_prevention_027",
        query="포도 유기재배에서 병 발생을 줄이기 위해 기본적으로 어떤 재배환경을 만들어야 하나요?",
        gt_answer=(
            "병 발생은 포도원 내부의 온도와 잎·줄기 표면의 습도 지속시간에 밀접하게 "
            "관련된다. 수관이 복잡하면 햇빛과 바람이 통하지 않고 비가 온 뒤 쉽게 마르지 "
            "않아 병 발생 환경이 조성되므로 수관 내부의 채광과 통풍을 개선해야 한다. "
            "토양이 과습하면 나무가 연약해지고 병 저항성이 낮아지므로 배수를 철저히 하며, "
            "나무를 도장시키는 질소 과다 시비도 피해야 한다."
        ),
        source_pages=(45,),
        required_terms=("습도 지속시간", "배수관리", "질소시비 과다"),
    ),
    EvaluationCase(
        query_id="grape_climate_site_028",
        query="유기 포도원을 새로 만들 때 입지 선정에서 가장 먼저 고려할 조건과 서리 대책은 무엇인가요?",
        gt_answer=(
            "포도원 개원 시 첫째로 고려할 사항은 재배지의 기상조건이다. 포도는 품종에 따라 "
            "내한성 차이가 크므로 추운 지역에서는 내한성이 강한 품종을 선택해야 한다. "
            "발아 이후 개화기 무렵 늦서리 피해도 고려해야 하며, 찬 공기의 이동 통로나 "
            "정체지역처럼 상습적으로 서리피해가 발생하는 곳에는 방상팬을 설치하거나 "
            "방상림을 조성하는 등의 대책을 세운다."
        ),
        source_pages=(14, 15),
        required_terms=("기상조건", "늦서리", "방상팬"),
    ),
    EvaluationCase(
        query_id="grape_new_land_fertility_029",
        query="새로 개간한 포도밭의 땅심을 높이려면 어떤 작업을 해야 하며 퇴구비 사용 시 무엇을 주의해야 하나요?",
        gt_answer=(
            "새로 개간한 토양은 우선 50cm 이상 깊이갈이를 실시하고 거친 퇴비를 많이 "
            "시용해 땅심을 높인다. 땅심이 좋은 토양은 단순히 질소가 많은 토양이 아니라 "
            "토양이 부드러워 뿌리가 잘 자라고, 유기물 함량이 높아 수분 보유력과 화학성분에 "
            "대한 완충력이 높은 토양이다. 가축분뇨 퇴구비는 충분히 부숙되지 않으면 "
            "질소성분만 높이고 토양 속에서 발효하며 가스를 발생시켜 부작용을 일으킬 수 있다."
        ),
        source_pages=(15, 16),
        required_terms=("50cm 이상", "깊이갈이", "가스를 발생"),
    ),
    EvaluationCase(
        query_id="grape_planting_time_030",
        query="포도 묘목은 가을과 봄 중 언제 심는 것이 좋으며 봄 식재는 언제까지 해야 하나요?",
        gt_answer=(
            "아주 추운 지방이 아니라면 낙엽 직후인 가을에 심는 것이 나무 발육에 좋다. "
            "가을에 심으면 다음 해 봄까지 뿌리가 흙에 자리 잡고 새 뿌리가 내려 발아가 "
            "빠르고 생육에도 유리하다. 봄에 심을 때는 땅이 풀린 뒤 늦어도 3월 하순까지 "
            "심어야 한다."
        ),
        source_pages=(16, 17),
        required_terms=("낙엽 직후", "3월 하순", "발아도 빠르며"),
    ),
    EvaluationCase(
        query_id="grape_planting_distance_031",
        query="캠벨얼리와 거봉계 포도의 권장 재식거리와 초기 재식 후 간벌 기준은 어떻게 다른가요?",
        gt_answer=(
            "캠벨얼리처럼 단초전정이 가능한 품종을 개량일자형 수형으로 재배할 때는 열간과 "
            "주간을 각각 2.7m로 하여 10a당 137주를 심는 것이 권장된다. 거봉계 품종처럼 "
            "덕식으로 재배할 때는 3.3×3.3m로 10a당 92주를 심는다. 거봉계는 식재 후 "
            "4~5년부터 간벌하여 주간거리를 확대해야 한다."
        ),
        source_pages=(17,),
        required_terms=("2.7×주간 2.7m", "10a 당 92주", "4~5년"),
    ),
    EvaluationCase(
        query_id="grape_mba_032",
        query="MBA 포도 품종의 숙기와 품질 특성, 유기재배 시 주의할 점은 무엇인가요?",
        gt_answer=(
            "머스캣 베일리 에이(MBA)는 10월 상순에 익는 만생종으로 과방중은 500g 이상, "
            "과립중은 약 5g이고 당도는 약 19°Bx이다. 생식과 양조를 겸할 수 있고 수세가 "
            "왕성하며 착립과 착색이 좋은 편이다. 유기재배에서도 마스캇향이 강하고 열과 등 "
            "생리장해가 적지만, 과방중이 500g을 넘으면 착색이 늦어질 수 있다. 착색기간이 "
            "길어 미숙과를 수확하기 쉬우므로 숙기 판정을 철저히 하고 새눈무늬병에도 "
            "주의한다."
        ),
        source_pages=(21, 25),
        required_terms=("10월 상순", "500g이 넘으면", "새눈무늬병"),
    ),
    EvaluationCase(
        query_id="grape_green_manure_comparison_033",
        query="헤어리베치와 호밀은 포도밭 녹비작물로서 양분 공급과 토양 개선 효과가 어떻게 다른가요?",
        gt_answer=(
            "헤어리베치는 콩과 녹비작물로 공중질소를 고정하고 분해가 빨라 후작물이 양분을 "
            "쉽게 이용할 수 있으며, 생체수량과 질소 공급량도 큰 편이다. 호밀은 화본과 "
            "작물로 생체량이 많고 가뭄과 저온에 강하며 토양 유기물 증가와 물리성 개선 "
            "효과가 크다. 다만 헤어리베치보다 질소 함량이 낮아 질소 공급원보다는 토양 "
            "물리성 개선 용도로 적합하다."
        ),
        source_pages=(30, 31, 32),
        required_terms=("헤어리베치", "호밀", "물리성 개선"),
    ),
    EvaluationCase(
        query_id="grape_nitrogen_sources_034",
        query="포도 유기재배에서 질소를 공급할 때 사용할 수 있는 자재와 기본 관리 원칙은 무엇인가요?",
        gt_answer=(
            "유기재배에서는 화학비료를 사용할 수 없으므로 녹비작물과 허용 유기농자재로 "
            "양분을 공급한다. 질소의 최소 요구량은 퇴비와 녹비로 공급할 수 있으며 추가 "
            "질소원으로 유박 등 유기질비료, 생선액비와 혈분을 사용할 수 있다. 표준시비량은 "
            "평균값이므로 실제 토양을 검정해 양분 요구량을 결정하는 토양검정시비가 더 "
            "권장된다. 유기자재는 토양 pH와 배수 등 토양 상태를 건전하게 만든 뒤 사용한다."
        ),
        source_pages=(32, 33),
        required_terms=("생선액비", "혈분", "토양검정시비량"),
    ),
    EvaluationCase(
        query_id="grape_phosphorus_potassium_sources_035",
        query="포도 유기재배에서 인산과 칼륨이 부족할 때 어떤 허용 자재로 보충할 수 있나요?",
        gt_answer=(
            "인산은 계분이나 돈분 퇴비와 피복작물로 공급할 수 있고, 추가 공급이 필요하면 "
            "인증된 유기농자재인 인광석을 이용한다. 칼륨은 착립 이후 흡수가 증가하고 특히 "
            "경핵기 이후 포도알 비대기에 많이 흡수된다. 칼륨 공급에는 퇴비, 유기축사에서 "
            "사용한 짚, 화강암 분말, 해조분과 나뭇재 등을 이용할 수 있다."
        ),
        source_pages=(33, 34),
        required_terms=("인광석", "화강암 분말", "해조분"),
    ),
    EvaluationCase(
        query_id="grape_organic_definition_036",
        query="이 매뉴얼에서는 유기농업을 어떤 농업체계로 정의하며 생산 과정에서 어떤 자재를 사용한다고 설명하나요?",
        gt_answer=(
            "유기농업은 농업생태계의 건강을 증진하고 생물종의 다양성, 생물순환과 생물활동을 "
            "높이기 위한 총체적 농업체계로 정의된다. 화학비료, 유기합성농약, 가축사료첨가제 "
            "등 합성 화학물질을 사용하지 않고 유기물, 자연광물과 미생물 등 물리적·생물적으로 "
            "제조된 자재를 이용해 안전한 농축산물을 생산하고 농업생태계를 유지·보전한다. "
            "지역 특성을 고려한 관리와 농장 외부 자원 투입의 자제도 강조한다."
        ),
        source_pages=(5,),
        required_terms=("총체적 농업체계", "화학비료", "농장 외부 자원"),
    ),
    EvaluationCase(
        query_id="grape_conventional_organic_037",
        query="관행농업과 유기농업은 종자 선택, 작부체계, 토양 양분관리와 병해충 관리에서 어떻게 다른가요?",
        gt_answer=(
            "관행농업은 생산성 중심의 품종과 단작을 선호하고 화학비료와 화학농약을 활용하며 "
            "GMO 재배가 가능하다. 유기농업은 내병충성과 양분이용 효율이 좋은 품종을 고르고 "
            "GMO를 재배하지 않으며, 윤작·간작·혼작으로 종 다양성을 유지한다. 토양 양분은 "
            "두과 녹비, 심근성 작물과 지역 내 양분자원을 이용해 순환시키며, 병해충과 잡초는 "
            "천적·미생물·저항성 품종·유기물 멀칭·예취·화염제초 등으로 관리한다."
        ),
        source_pages=(6,),
        required_terms=("GMO 재배 불가", "윤작, 간작, 혼작", "화염제초"),
    ),
    EvaluationCase(
        query_id="grape_organic_certification_038",
        query="매뉴얼에 제시된 2017년 국내 유기농업과 무농약 농업의 인증면적, 농가수와 출하량은 얼마인가요?",
        gt_answer=(
            "2017년 국내 유기농업 인증면적은 20,673ha이고 무농약 인증면적은 "
            "59,644ha로 제시되어 있다. 유기농업 농가수는 13,379호, 무농약 농가수는 "
            "46,044호였으며 출하량은 유기농산물 113,526톤, 무농약농산물 "
            "382,855톤이었다. 같은 해 전체 친환경농산물 출하량은 496,381톤이며 "
            "과실류는 27,271톤으로 5.5%를 차지했다."
        ),
        source_pages=(7, 8),
        required_terms=("20,673ha", "13,379호", "496,381톤"),
    ),
    EvaluationCase(
        query_id="grape_industry_2018_039",
        query="매뉴얼이 설명한 2018년 국내 포도 재배면적과 생산량, 주요 재배지역 및 작형 비중은 어떠했나요?",
        gt_answer=(
            "2018년 국내 포도 재배면적은 12,795ha로 전년보다 2% 감소했고, 주산지인 "
            "경상북도가 전국 면적의 53%를 차지했다. 생산량은 재배면적 감소와 함께 2000년 "
            "47만 6천 톤에서 2018년 21만 5천 톤으로 감소했다. 2018년 작형별 면적 비중은 "
            "노지재배 85.5%, 시설재배 14.5%였다."
        ),
        source_pages=(9, 10),
        required_terms=("12,795ha", "전국의 53%", "85.5%"),
    ),
    EvaluationCase(
        query_id="grape_trade_2018_040",
        query="2018년 신선포도의 수출량과 주요 수출 변화, 수입량과 주요 수입국 비중은 어떻게 기록되어 있나요?",
        gt_answer=(
            "2018년 신선포도 수출량은 1,275톤이었다. 기존 주요 수출국인 미국과 홍콩 외에 "
            "싱가포르와 베트남 등 동남아시아로의 수출이 증가했고, 2018년 수출량은 베트남 "
            "329톤, 홍콩 314톤, 싱가포르 187톤, 미국 129톤이었다. 수입량은 약 6만 톤인 "
            "59,998톤이었으며 칠레가 31,966톤으로 53.3%, 미국이 19,653톤으로 32.8%를 "
            "차지했다."
        ),
        source_pages=(10, 11),
        required_terms=("1,275톤", "59,998", "53.3"),
    ),
    EvaluationCase(
        query_id="grape_supply_outlook_041",
        query="매뉴얼의 전망표에서 2028년 포도 재배면적, 생산량, 수입량과 1인당 소비량은 얼마로 전망했나요?",
        gt_answer=(
            "매뉴얼은 포도 재배면적이 2023년 13,500ha까지 증가한 뒤 2028년에는 "
            "12,400ha 수준이 될 것으로 전망했다. 2028년 생산량은 18만 2천 톤, 수입량은 "
            "6만 9천 톤으로 전망했으며, 1인당 연간 소비량은 2018년 4.3kg에서 2028년 "
            "4.7kg으로 증가할 것으로 제시했다."
        ),
        source_pages=(12,),
        required_terms=("12,400ha", "69천톤", "4.7kg"),
    ),
    EvaluationCase(
        query_id="grape_species_comparison_042",
        query="유럽종, 미국종과 교잡종 포도는 당도·과립 크기·내한성·향기와 내병성에서 어떤 차이가 있나요?",
        gt_answer=(
            "유럽종은 당도가 높고 포도알과 송이가 크며 머스캇향을 가지지만 내한성이 약하다. "
            "노균병, 세균무늬병과 흰가루병에는 약하고 갈반병에는 강한 것으로 제시된다. "
            "미국종은 당도와 과립·송이 크기는 작지만 내한성이 강하고 여우향을 가지며, "
            "노균병·세균무늬병·흰가루병에는 강하지만 갈반병에는 약하다. 미국종과 유럽종의 "
            "교잡종은 당도, 크기, 내한성과 내병성이 대체로 중간 수준이다."
        ),
        source_pages=(18,),
        required_terms=("유럽종", "미국종", "여우향"),
    ),
    EvaluationCase(
        query_id="grape_campbell_early_043",
        query="캠벨얼리 품종의 수세·내한성·숙기와 과실 특성, 재배 시 주의점은 무엇인가요?",
        gt_answer=(
            "캠벨얼리는 수세가 중간이고 내한성이 매우 강하지만 뿌리가 얕은 천근성이라 "
            "내건성이 약해 토양수분이 적으면 생육이 떨어진다. 숙기는 8월 하순~9월 상순인 "
            "조생종이고 과방은 약 370g, 과립은 약 5g이며 완숙 당도는 약 15.5°Bx이다. "
            "결실성이 좋지만 과다 결실하면 수세와 품질이 떨어지고 꽃떨이가 생기기 쉽다. "
            "질소 과다를 피하고 개화 1주일 전 웃자란 신초를 10~11매 남기고 순지르기한다."
        ),
        source_pages=(20,),
        required_terms=("내한성은 매우 강", "15.5°Bx", "전엽 10~11매"),
    ),
    EvaluationCase(
        query_id="grape_kyoho_044",
        query="거봉 품종의 숙기와 과실 품질, 온도와 착과량에 따른 문제 및 수세관리 방법은 무엇인가요?",
        gt_answer=(
            "거봉은 9월 중순에 익고 과방은 약 400g, 과립은 11g 이상이며 당도는 약 "
            "19°Bx이다. 육질이 연하고 과즙과 과분이 많지만 고온에서는 착색이 불량하고 "
            "저온에서는 신맛이 강해진다. 수세가 강해 꽃떨이, 열과와 착색 불량이 문제가 될 "
            "수 있다. 유핵재배에서는 강전정과 질소 과다를 피하고, 결실이 지나치면 착색과 "
            "품질이 떨어지므로 송이 수와 크기를 조절한다."
        ),
        source_pages=(20, 21),
        required_terms=("11g 이상", "고온시 착색 불량", "강전정을 피하고"),
    ),
    EvaluationCase(
        query_id="grape_delaware_045",
        query="델라웨어 품종의 숙기, 송이와 과립 크기, 당도·산도 및 시설재배 활용 특성은 무엇인가요?",
        gt_answer=(
            "델라웨어는 9월 상순에 익으며 과방중은 약 90g, 과립중은 약 1.5g인 소립·소과 "
            "품종이다. 당도는 약 19.2°Bx이고 산 함량은 0.2%로 식미가 우수하다. 성숙일수가 "
            "짧고 연속해서 조기 가온해도 수세가 저하되지 않으며 품질이 좋아 조기가온재배에 "
            "많이 이용된다."
        ),
        source_pages=(21,),
        required_terms=("과방중과 과립중이 각각 90g, 1.5g", "19.2°Bx", "조기가온재배"),
    ),
    EvaluationCase(
        query_id="grape_shine_muscat_traits_046",
        query="샤인머스켓 품종의 과실 특성, 적정 착과량과 유목기 재배관리상 주의점은 무엇인가요?",
        gt_answer=(
            "샤인머스켓은 황녹색 대립 품종으로 식감과 머스켓향이 좋고 당도가 높으며 산도가 "
            "낮다. 포도알 무게는 약 8~12g이고 거봉보다 추위에 강하며 저장기간이 길다. "
            "착과량이 적을수록 과립 모양과 당도가 균일하고 당도가 빨리 올라 수확기가 "
            "짧아진다. 10a당 1,800kg 수확 조건에서는 만개 후 약 110일에 18°Bx 이상으로 "
            "수확 가능하다. 수세가 강하므로 유목기부터 수관을 확장하고 질소 시비를 조심한다."
        ),
        source_pages=(21, 22),
        required_terms=("8~12g", "1,800kg", "생육일수 110일"),
    ),
    EvaluationCase(
        query_id="grape_hongju_seedless_047",
        query="홍주씨들리스 품종의 숙기와 과실 특성, 적합한 재배환경과 월동관리 방법은 무엇인가요?",
        gt_answer=(
            "홍주씨들리스는 9월 하순에 익는 만생 무핵 품종으로 과방중은 약 538g, 과립중은 "
            "5.4g이며 당도는 18.4°Bx이다. 과육이 아삭하고 착립과 착방이 좋으며 열과와 "
            "꽃떨이가 매우 적다. 고품질 생산에는 비가림 또는 하우스재배가 권장되고 적숙기까지 "
            "잎을 건전하게 유지해야 한다. 수세가 매우 강해 중장초 전정과 5m 이상의 "
            "주간거리가 필요하다. 내한성이 약하므로 온난한 남부 평야나 시설재배가 적합하고 "
            "피복 또는 매몰 월동조치가 필요하다."
        ),
        source_pages=(22,),
        required_terms=("홍주씨들리스", "538g", "주간거리 5m이상"),
    ),
    EvaluationCase(
        query_id="grape_rain_shelter_048",
        query="캠벨얼리와 거봉 유핵재배에서 권장하는 덕 높이와 비가림시설 폭·간격은 어떻게 다른가요?",
        gt_answer=(
            "캠벨얼리 같은 중소립계 품종의 비가림시설은 덕 높이 180cm, 주지 유인선 높이 "
            "150cm인 개량식일자형이 표준이며 비가림 폭은 240cm, 비가림 사이 간격은 "
            "30cm이다. 거봉 대립계 유핵재배는 열간과 주간 간격이 3.3×3.3m인 경우 "
            "비가림 폭을 300cm로 넓게 설치하며, 기존 덕면과 비가림 사이는 30cm 이상 "
            "띄운다."
        ),
        source_pages=(17,),
        required_terms=("덕높이 180cm", "비가림 폭은 240cm", "300cm"),
    ),
    EvaluationCase(
        query_id="grape_organic_soil_principles_049",
        query="포도 유기재배에서 제시하는 토양관리의 목표, 원칙과 실행 방안은 무엇인가요?",
        gt_answer=(
            "토양관리의 목표는 토양환경의 생태학적 건전성을 유지하고 토양 퇴화·유실과 "
            "환경오염을 최소화하면서 작물 생산성을 최적화하는 것이다. 자연의 순환기능을 "
            "강화하고 영농활동의 생태적 교란을 줄이며, 녹비작물 작부체계와 적정 유기물 "
            "투입으로 토양비옥도를 유지하고 생물다양성을 높인다. 실행 방안에는 유기농·축 "
            "부산물 활용, 녹비와 피복작물 재배, 식물잔사 순환, 재배적 토양보존과 허용자재를 "
            "이용한 양분관리가 포함된다."
        ),
        source_pages=(26,),
        required_terms=("생태학적 건전성", "화석연료", "식물잔사의 순환"),
    ),
    EvaluationCase(
        query_id="grape_ground_cover_trial_050",
        query="자연 초생재배와 부직포·흑색비닐 피복 시험에서 포도 열과율, 안토시아닌과 수량은 어떻게 달랐나요?",
        gt_answer=(
            "자연 초생재배의 열과율은 5.6%, 총 안토시아닌은 837mg/kg, 수량은 "
            "10a당 1,199kg이었다. 초생재배에 부직포를 피복한 경우 열과율은 2.8%로 가장 "
            "낮았고 안토시아닌은 1,341mg/kg, 수량은 1,426kg/10a였다. 흑색비닐 피복은 "
            "열과율 3.9%, 안토시아닌 1,434mg/kg으로 가장 높았으며 수량은 "
            "1,410kg/10a였다."
        ),
        source_pages=(29,),
        required_terms=("열과율", "1,341", "1,434"),
    ),
)


class NongSabuClient:
    def __init__(self, base_url: str, timeout: int) -> None:
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.session = requests.Session()

    def register_or_login(self, email: str, password: str, full_name: str) -> None:
        signup_response = self.session.post(
            f"{self.base_url}/api/v1/auth/signup",
            json={"email": email, "password": password, "fullName": full_name},
            timeout=self.timeout,
        )

        if signup_response.ok:
            auth_data = unwrap_data(signup_response)
        else:
            login_response = self.session.post(
                f"{self.base_url}/api/v1/auth/login",
                json={"email": email, "password": password},
                timeout=self.timeout,
            )
            auth_data = unwrap_data(login_response)

        token = auth_data.get("accessToken")
        if not token:
            raise RuntimeError("인증 응답에 accessToken이 없습니다.")
        self.session.headers["Authorization"] = f"Bearer {token}"

    def upload_document(self, pdf_path: Path) -> None:
        with pdf_path.open("rb") as pdf_file:
            response = self.session.post(
                f"{self.base_url}/api/v1/documents",
                files={"file": (pdf_path.name, pdf_file, "application/pdf")},
                timeout=max(self.timeout, 300),
            )
        unwrap_data(response)

    def search(self, query: str, top_k: int) -> list[dict[str, Any]]:
        response = self.session.post(
            f"{self.base_url}/api/v1/rag/search",
            json={"query": query, "topK": top_k},
            timeout=self.timeout,
        )
        data = unwrap_data(response)
        items = data.get("items", data) if isinstance(data, dict) else data
        if not isinstance(items, list):
            raise RuntimeError("검색 응답의 items가 배열이 아닙니다.")
        return items

    def ask(self, question: str) -> str:
        response = self.session.post(
            f"{self.base_url}/api/v1/rag/ask",
            json={"question": question},
            timeout=max(self.timeout, 180),
        )
        data = unwrap_data(response)
        answer = data.get("answer") if isinstance(data, dict) else None
        if not answer:
            raise RuntimeError("RAG 답변 응답에 answer가 없습니다.")
        return str(answer)


def unwrap_data(response: requests.Response) -> Any:
    try:
        body = response.json()
    except ValueError as exc:
        raise RuntimeError(
            f"API가 JSON이 아닌 응답을 반환했습니다: HTTP {response.status_code}"
        ) from exc

    if not response.ok:
        message = body.get("message", body) if isinstance(body, dict) else body
        raise RuntimeError(f"API 요청 실패: HTTP {response.status_code} - {message}")

    if isinstance(body, dict) and "data" in body:
        return body["data"]
    return body


def validate_cases_against_pdf(pdf_path: Path) -> None:
    reader = PdfReader(str(pdf_path))
    page_count = len(reader.pages)

    for case in CASES:
        if max(case.source_pages) > page_count:
            raise RuntimeError(
                f"{case.query_id}: PDF는 {page_count}쪽인데 "
                f"{max(case.source_pages)}쪽을 참조합니다."
            )

        source_text = "\n".join(
            reader.pages[page_number - 1].extract_text() or ""
            for page_number in case.source_pages
        )
        missing_terms = [term for term in case.required_terms if term not in source_text]
        if missing_terms:
            raise RuntimeError(
                f"{case.query_id}: 원문 {case.source_pages}쪽에서 다음 근거 표현을 "
                f"찾지 못했습니다: {', '.join(missing_terms)}"
            )


def to_context(item: dict[str, Any], index: int) -> dict[str, str]:
    text = item.get("content") or item.get("text")
    if not text:
        raise RuntimeError(f"{index}번째 검색 결과에 content/text가 없습니다.")

    doc_id = (
        item.get("chunkId")
        or item.get("docId")
        or item.get("id")
        or f"retrieved_chunk_{index}"
    )
    return {"doc_id": str(doc_id), "text": str(text)}


def build_dataset(
    client: NongSabuClient,
    top_k: int,
) -> dict[str, list[dict[str, Any]]]:
    results: list[dict[str, Any]] = []

    for case in CASES:
        print(f"[{case.query_id}] 검색 및 답변 생성 중...")
        retrieved_items = client.search(case.query, top_k)
        answer = client.ask(case.query)
        results.append(
            {
                "query_id": case.query_id,
                "query": case.query,
                "gt_answer": case.gt_answer,
                "response": answer,
                "retrieved_context": [
                    to_context(item, index)
                    for index, item in enumerate(retrieved_items, start=1)
                ],
            }
        )

    return {"results": results}


def parse_args() -> argparse.Namespace:
    default_pdf = Path.home() / "Downloads" / "2019 포도 유기재배 메뉴얼 ebook.pdf"
    default_output = (
        Path(__file__).resolve().parent
        / "output"
        / "grape_manual_ragchecker.json"
    )

    parser = argparse.ArgumentParser(
        description="포도 유기재배 PDF 기반 RAGChecker 평가 데이터셋을 생성합니다."
    )
    parser.add_argument("--pdf", type=Path, default=default_pdf)
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--email", default="rag-eval@nongsabu.local")
    parser.add_argument("--password", default="RagEval123!")
    parser.add_argument("--full-name", default="RAG Evaluation")
    parser.add_argument("--top-k", type=int, default=4)
    parser.add_argument("--timeout", type=int, default=60)
    parser.add_argument("--output", type=Path, default=default_output)
    parser.add_argument(
        "--skip-upload",
        action="store_true",
        help="동일 평가 계정에 PDF가 이미 적재된 경우 업로드를 생략합니다.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    pdf_path = args.pdf.expanduser().resolve()
    output_path = args.output.expanduser().resolve()

    if not pdf_path.is_file():
        print(f"PDF 파일을 찾을 수 없습니다: {pdf_path}", file=sys.stderr)
        return 1
    if args.top_k < 1:
        print("--top-k는 1 이상이어야 합니다.", file=sys.stderr)
        return 1

    try:
        print("PDF 원문과 평가 문항의 근거를 확인합니다...")
        validate_cases_against_pdf(pdf_path)

        client = NongSabuClient(args.base_url, args.timeout)
        client.register_or_login(args.email, args.password, args.full_name)

        if not args.skip_upload:
            print(f"PDF 업로드 및 임베딩 중: {pdf_path.name}")
            client.upload_document(pdf_path)

        dataset = build_dataset(client, args.top_k)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(
            json.dumps(dataset, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
    except (requests.RequestException, RuntimeError) as exc:
        print(f"생성 실패: {exc}", file=sys.stderr)
        return 1

    print(f"완료: {len(dataset['results'])}개 평가 항목")
    print(f"출력 파일: {output_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
