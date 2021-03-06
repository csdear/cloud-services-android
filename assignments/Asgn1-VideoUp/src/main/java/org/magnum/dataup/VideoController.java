/*
 *
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.magnum.dataup;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class VideoController {
    private static final AtomicLong currentId = new AtomicLong(0L);
    private Map<Long, Video> videos = new HashMap<Long, Video>();


    // HTTP Request Handling - GET Methods
    @RequestMapping(method = RequestMethod.GET, value = "/video")
    public
    @ResponseBody
    Collection<Video> getVideoList() {
        return videos.values();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/video/{id}/data")
    public void getData(
            @PathVariable("id") long id,
            HttpServletResponse response)
            throws IOException {
        Video v = videos.get(new Long(id));
        if (v != null && VideoFileManager.get().hasVideoData(v)) {
            VideoFileManager.get().copyVideoData(v, response.getOutputStream());
            response.setStatus(HttpServletResponse.SC_OK);
        } else
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    // HTTP Request Handling - POST methods
    @RequestMapping(method = RequestMethod.POST, value = "/video")
    public
    @ResponseBody
    Video addVideo(@RequestBody Video v) {
        save(v);
        return v;
    }


    @RequestMapping(method = RequestMethod.POST, value = "/video/{id}/data")
    public
    @ResponseBody
    VideoStatus setVideoData(
            @PathVariable("id") long id,
            @RequestParam("data") MultipartFile videoData,
            HttpServletResponse response)
            throws IOException {
        VideoStatus status = null;
        Video v = videos.get(new Long(id));
        if (v != null) {
            InputStream in = videoData.getInputStream();
            VideoFileManager.get().saveVideoData(v, in);
            status = new VideoStatus(VideoState.READY);
        } else
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return status;
    }

    // helper methods.
    //You can use a method like the following to figure out the address of your server and generate a data url for a video:
    private String getDataUrl(long videoId) {
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

    private String getUrlBaseForLocalServer() {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String base =
                "http://" + request.getServerName()
                        + ((request.getServerPort() != 80) ? ":" + request.getServerPort() : "");

        return base;
    }

    //Helper methods
    //sets a uniqueId for a video
    public Video save(Video entity) {
        checkAndSetId(entity);
        videos.put(entity.getId(), entity);
        return entity;
    }

    private void checkAndSetId(Video entity) {
        if (entity.getId() == 0) {
            entity.setId(currentId.incrementAndGet());
        }

    }
}
