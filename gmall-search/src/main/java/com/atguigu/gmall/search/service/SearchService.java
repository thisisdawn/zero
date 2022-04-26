package com.atguigu.gmall.search.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchService {
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    public SearchResponseVo search(SearchParamVo searchParamVo) {
        try {
            //执行搜索
            SearchRequest request = new SearchRequest(new String[]{"goods"}, this.buildDsl(searchParamVo));
            SearchResponse response =  this.restHighLevelClient.search(request, RequestOptions.DEFAULT);
            //解析结果集
            SearchResponseVo responseVo = this.parseResult(response);
//            设置分页参数
            responseVo.setPageNum(searchParamVo.getPageNum());
            responseVo.setPageSize(searchParamVo.getPagesize());
            return responseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }







    private SearchResponseVo parseResult(SearchResponse response){
        SearchResponseVo responseVo = new SearchResponseVo();

        // 1.解析搜索结果集
        SearchHits hits = response.getHits();
        TotalHits totalHits = hits.getTotalHits();
        if (totalHits != null) {
            responseVo.setTotal(totalHits.value);
        }
        // 解析出当前页的数据
        SearchHit[] hitsHits = hits.getHits();
        // 把hitsHits数组转化成 GoodsList集合
        responseVo.setGoodsList(Arrays.stream(hitsHits).map(hitsHit -> {
            // 获取每个命中结果集中的_source
            String json = hitsHit.getSourceAsString();
            Goods goods = JSON.parseObject(json, Goods.class);
            // 解析高亮结果集，把普通title覆盖掉
            Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
            goods.setTitle(highlightFields.get("title").fragments()[0].string());
            return goods;
        }).collect(Collectors.toList()));

        // 2.解析聚合结果集
        Aggregations aggregations = response.getAggregations();
        // 解析品牌聚合结果集获取品牌列表
        ParsedLongTerms brandIdAgg = aggregations.get("brandIdAgg");
        List<? extends Terms.Bucket> buckets = brandIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(buckets)) {
            responseVo.setBrands(buckets.stream().map(bucket -> {
                BrandEntity brandEntity = new BrandEntity();
                // 从桶中获取brandId
                brandEntity.setId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                // 获取桶中的子聚合
                Aggregations subAgg = ((Terms.Bucket) bucket).getAggregations();
                // 获取品牌名称的子聚合
                ParsedStringTerms brandNameAgg = subAgg.get("brandNameAgg");
                brandEntity.setName(brandNameAgg.getBuckets().get(0).getKeyAsString());
                // 获取品牌logo的子聚合
                ParsedStringTerms logoAgg = subAgg.get("logoAgg");
                brandEntity.setLogo(logoAgg.getBuckets().get(0).getKeyAsString());
                return brandEntity;
            }).collect(Collectors.toList()));
        }

        // 解析分类聚合结果集获取分类列表
        ParsedLongTerms categoryIdAgg = aggregations.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryBuckets = categoryIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(categoryBuckets)){
            // 把分类id的桶集合 转化成分类的实体类集合
            responseVo.setCategories(categoryBuckets.stream().map(bucket -> {
                CategoryEntity categoryEntity = new CategoryEntity();
                categoryEntity.setId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                // 获取分类名称的子聚合
                ParsedStringTerms categoryNameAgg = ((Terms.Bucket) bucket).getAggregations().get("categoryNameAgg");
                categoryEntity.setName(categoryNameAgg.getBuckets().get(0).getKeyAsString());
                return categoryEntity;
            }).collect(Collectors.toList()));
        }

        // 解析规格参数的聚合结果集获取规格参数的过滤集合
        ParsedNested attrAgg = aggregations.get("attrAgg");
        // 获取嵌套聚合中的规格参数id的子聚合
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        // 获取规格参数id聚合中的桶
        List<? extends Terms.Bucket> attrIdBuckets = attrIdAgg.getBuckets();
        // 把桶集合转化成SearchResponseAttrVo集合
        if (!CollectionUtils.isEmpty(attrIdBuckets)){
            responseVo.setFilters(attrIdBuckets.stream().map(bucket -> {
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                // 桶中的key就是规格参数的id
                searchResponseAttrVo.setAttrId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                // 获取子聚合
                Aggregations subAgg = ((Terms.Bucket) bucket).getAggregations();
                // 获取规格参数名称的子聚合
                ParsedStringTerms attrNameAgg = subAgg.get("attrNameAgg");
                // 规格参数名称应该有且仅有一个桶
                searchResponseAttrVo.setAttrName(attrNameAgg.getBuckets().get(0).getKeyAsString());
                // 获取规格参数值得子聚合
                ParsedStringTerms attrValueAgg = subAgg.get("attrValueAgg");
                searchResponseAttrVo.setAttrValue(attrValueAgg.getBuckets().stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList()));
                return searchResponseAttrVo;
            }).collect(Collectors.toList()));
        }

        return responseVo;
    }


    private SearchSourceBuilder buildDsl(SearchParamVo paramVo){
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // 如果搜索关键字为空则直接结束，抛出异常
        String keyword = paramVo.getKeyword();
        if (StringUtils.isBlank(keyword)){
            // TODO: 打广告
            throw new RuntimeException("你没有检索条件");
        }

        // 1.构建搜索和过滤条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        searchSourceBuilder.query(boolQueryBuilder);
        // 1.1.匹配查询
        boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyword).operator(Operator.AND));

        // 1.2.过滤条件
        // 1.2.1. 品牌过滤
        List<Long> brandId = paramVo.getBrandId();
        if (!CollectionUtils.isEmpty(brandId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", brandId));
        }

        // 1.2.2. 分类过滤
        List<Long> categoryId = paramVo.getCategoryId();
        if (!CollectionUtils.isEmpty(categoryId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId", categoryId));
        }

        // 1.2.3. 价格区间过滤
        Double priceFrom = paramVo.getPriceFrom();
        Double priceTo = paramVo.getPriceTo();
        // 只要任何一个价格不为空，则需要范围过滤
        if (priceFrom != null || priceTo != null){
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            boolQueryBuilder.filter(rangeQuery);
            // 如果priceFrom不为空，则添加gte
            if (priceFrom != null) {
                rangeQuery.gte(priceFrom);
            }
            if (priceTo != null) {
                rangeQuery.lte(priceTo);
            }
        }

        // 1.2.4. 是否有货过滤
        Boolean store = paramVo.getStore();
        if (store != null) {
            // 实际开发 直接过滤有货的商品即可
            boolQueryBuilder.filter(QueryBuilders.termQuery("store", store));
        }

        // 1.2.5. 规格参数嵌套过滤
        List<String> props = paramVo.getProps(); // 获取规格参数的过滤条件：["5:128G-256G", "4:8G-12G"]
        if (!CollectionUtils.isEmpty(props)){
            props.forEach(prop -> { // 4:8G-12G
                // 使用：分割 获取规格参数id 及 8G-12G
                String[] attrs = StringUtils.split(prop, ":");
                // 判断分割后的数据是否为空 长度是否为2 并且第一位必须是数字
                if (attrs != null && attrs.length == 2 && StringUtils.isNumeric(attrs[0])){
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("searchAttrs", boolQuery, ScoreMode.None));
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId", attrs[0]));
                    boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue", StringUtils.split(attrs[1], "-")));
                }
            });
        }

        // 2.构建排序条件 1-价格降序 2-价格升序 3-销量降序 4-新品降序
        Integer sort = paramVo.getSort();
        switch (sort) {
            case 1: searchSourceBuilder.sort("price", SortOrder.DESC); break;
            case 2: searchSourceBuilder.sort("price", SortOrder.ASC); break;
            case 3: searchSourceBuilder.sort("sales", SortOrder.DESC); break;
            case 4: searchSourceBuilder.sort("createTime", SortOrder.DESC); break;
            default:
                searchSourceBuilder.sort("_score", SortOrder.DESC);
        }

        // 3.构建分页条件
        Integer pageNum = paramVo.getPageNum();
        Integer pagesize = paramVo.getPagesize();
        searchSourceBuilder.from((pageNum - 1) * pagesize);
        searchSourceBuilder.size(pagesize);

        // 4.构建高亮
        searchSourceBuilder.highlighter(new HighlightBuilder().field("title")
                .preTags("<font style='color:red'>").postTags("</font>"));

        // 5.构建聚合
        // 5.1. 品牌聚合
        searchSourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("logoAgg").field("logo")));

        // 5.2. 分类聚合
        searchSourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));

        // 5.3. 规格参数聚合
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "searchAttrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue"))));

        // 6.结果集过滤
        searchSourceBuilder.fetchSource(new String[]{"skuId", "title", "subtitle", "price", "defaultImage"}, null);
        System.out.println(searchSourceBuilder);
        return searchSourceBuilder;
    }


}
