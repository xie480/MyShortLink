/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.yilena.myShortLink.project.service.impl;


import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.yilena.myShortLink.project.common.convention.exception.SystemException;
import org.yilena.myShortLink.project.service.UrlTitleService;

import java.net.URI;

/**
 * URL 标题接口实现层
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
@Service
@RequiredArgsConstructor
public class UrlTitleServiceImpl implements UrlTitleService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getTitleByUrl(String url) {
        try {
            // 获取网页内容
            String html = restTemplate.getForObject(url, String.class);
            Document doc = Jsoup.parse(html);
            // 获取标题
            return doc.title();
        } catch (Exception e) {
            throw new SystemException(STR."获取网站信息失败: \{e.getMessage()}");
        }
    }

    @Override
    public String getFaviconByUrl(String url) {
        try {
            // 获取网页内容
            String html = restTemplate.getForObject(url, String.class);
            Document doc = Jsoup.parse(html);
            // 获取图标
            return findFaviconUrl(doc, url);
        } catch (Exception e) {
            throw new SystemException(STR."获取网站信息失败: \{e.getMessage()}");
        }
    }

    private String findFaviconUrl(Document doc, String baseUrl) {
        // 查找可能的图标链接
        Element iconLink = doc.select("link[rel~=icon], link[rel~=shortcut]").first();
        if (iconLink != null) {
            String href = iconLink.attr("href");
            return resolveAbsoluteUrl(baseUrl, href);
        }
        // 尝试默认路径
        return resolveAbsoluteUrl(baseUrl, "/favicon.ico");
    }

    private String resolveAbsoluteUrl(String baseUrl, String path) {
        try {
            return new URI(baseUrl).resolve(path).toString();
        } catch (Exception e) {
            return STR."\{baseUrl}\{path.startsWith("/") ? path : STR."/\{path}"}";
        }
    }
}
