/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.social.movies.netflix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.social.oauth1.ProtectedResourceClientFactory;
import org.springframework.web.client.RestTemplate;

public class NetFlixTemplate implements NetFlixApi {
	
	private final RestTemplate restTemplate;

	private final String userBaseUrl;

	public NetFlixTemplate(String apiKey, String apiSecret, String accessToken, String accessTokenSecret) {
		this.restTemplate = ProtectedResourceClientFactory.create(apiKey, apiSecret, accessToken, accessTokenSecret);
		this.userBaseUrl = getUserBaseUrl();
	}
	
	public List<CatalogTitle> searchForTitles(String searchTerm) {
		Map<String, Object> resultMap = restTemplate.getForObject(SEARCH_TITLES_URL, Map.class, searchTerm);
		Map<String, Object> titlesMap = (Map<String, Object>) resultMap.get("catalog_titles");
		List<Map<String, Object>> titlesList = (List<Map<String, Object>>) titlesMap.get("catalog_title");
		List<CatalogTitle> titles = new ArrayList<CatalogTitle>();
		for (Map<String, Object> titleMap : titlesList) {
			String id = String.valueOf(titleMap.get("id"));
			String title = ((Map<String, String>) titleMap.get("title")).get("regular");
			String releaseYear = String.valueOf(titleMap.get("release_year"));
			titles.add(new CatalogTitle(id, title, releaseYear));
		}
		return titles;
	}

	public List<QueueItem> getDiscQueue() {
		Map<String, Object> resultMap = restTemplate.getForObject(userBaseUrl + QUEUE_PATH, Map.class);
		Map<String, Object> queueMap = (Map<String, Object>) resultMap.get("queue");
		List<Map<String, Object>> queueItemMaps = (List<Map<String, Object>>) queueMap.get("queue_item");
		List<QueueItem> queueItems = new ArrayList<QueueItem>();
		for (Map<String, Object> queueItemMap : queueItemMaps) {
			List<Map<String, String>> links = ((List<Map<String, String>>) queueItemMap.get("link"));
			String itemId = findItemId(links);
			String title = String.valueOf(((Map) queueItemMap.get("title")).get("regular"));
			String boxArtUrl = String.valueOf(((Map) queueItemMap.get("box_art")).get("small"));
			String releaseYear = String.valueOf(queueItemMap.get("release_year"));
			queueItems.add(new QueueItem(itemId, title, releaseYear, boxArtUrl));
		}
		return queueItems;
	}

	private String findItemId(List<Map<String, String>> linkList) {
		for (Map<String, String> linkMap : linkList) {
			if(linkMap.get("rel").equals("http://schemas.netflix.com/catalog/title")) {
				return linkMap.get("href");
			}
		}
		return "";
	}
	
	private String getUserBaseUrl() {
		Map<String, Map<String, Map<String, String>>> result = restTemplate.getForObject(CURRENT_USER_URL, Map.class);
		return result.get("resource").get("link").get("href");
	}

	private static final String SEARCH_TITLES_URL = "http://api.netflix.com/catalog/titles?term={term}&max_results=5&output=json";
	private static final String CURRENT_USER_URL = "http://api.netflix.com/users/current?output=json";
	private static final String QUEUE_PATH = "/queues/disc?output=json";
}
