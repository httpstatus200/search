package com.dongcheol.search.infra.place.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

//   {
//       "address_name": "서울 영등포구 여의도동 36-3",
//       "category_group_code": "BK9",
//       "category_group_name": "은행",
//       "category_name": "금융,보험 > 금융서비스 > 은행 > KB국민은행",
//       "distance": "",
//       "id": "10142322",
//       "phone": "02-2073-7114",
//       "place_name": "KB국민은행 여의도영업부",
//       "place_url": "http://place.map.kakao.com/10142322",
//       "road_address_name": "서울 영등포구 국제금융로8길 26",
//       "x": "126.927887551769",
//       "y": "37.5208765741827"
//       },

//    {
//        "title": "KB<b>국민은행 여의도</b>영업부",
//        "link": "https://obank.kbstar.com/",
//        "category": "금융,보험>은행",
//        "description": "",
//        "telephone": "",
//        "address": "서울특별시 영등포구 여의도동 36-3",
//        "roadAddress": "서울특별시 영등포구 국제금융로8길 26",
//        "mapx": "305431",
//        "mapy": "547064"
//        },

@Getter
@ToString
@Builder
public class PlaceInfo {

    private String name;
    private String address;
    private String roadAddress;
}
