/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.processor.jasper;

import org.mapfish.print.config.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Interprets the text as an image URL.
 *
 * @author Jesse on 6/30/2014.
 */
public final class HttpImageResolver implements TableColumnConverter<BufferedImage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpImageResolver.class);
    private static final int IMAGE_SIZE = 48;
    private Pattern urlExtractor = Pattern.compile("(.*)");
    private int urlGroup = 1;
    private BufferedImage defaultImage;

    @Autowired
    private ClientHttpRequestFactory requestFactory;


    /**
     * Constructor.
     */
    public HttpImageResolver() {
        this.defaultImage = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_4BYTE_ABGR);
    }

    /**
     * Sets the RegExp pattern to use for extracting the url from the text.  By default the whole string is used.
     *
     * For example: <code>.*&amp;img src="([^"]+)".*</code>
     *
     * @param pattern The url extractor regular expression.  Default is <code>"(.*)"</code>
     */
    public void setUrlExtractor(final String pattern) {
        this.urlExtractor = Pattern.compile(pattern);
    }

    /**
     * Select the group in the regular expression that contains the url.
     *
     * @param urlGroup the index of the group (starting at 1) that contans the url.
     */
    public void setUrlGroup(final int urlGroup) {
        this.urlGroup = urlGroup;
    }

    @Override
    public BufferedImage resolve(final String text) throws URISyntaxException, IOException {
        Matcher urlMatcher = this.urlExtractor.matcher(text);

        if (urlMatcher.matches() && urlMatcher.group(this.urlGroup) != null) {
            URI url = new URI(urlMatcher.group(this.urlGroup));
            final ClientHttpRequest request = this.requestFactory.createRequest(url, HttpMethod.GET);
            final ClientHttpResponse response = request.execute();
            if (response.getStatusCode() == HttpStatus.OK) {
                try {
                    return ImageIO.read(response.getBody());
                } catch (IOException e) {
                    LOGGER.warn("Image loaded from '" + url + "'is not valid: " + e.getMessage());
                }
            } else {
                LOGGER.warn("Error loading the table row image: " + url + ".\nStatus Code: " + response.getStatusCode() +
                "\nStatus Text: " + response.getStatusText());
            }
        }

        return this.defaultImage;
    }

    @Override
    public void validate(final List<Throwable> validationErrors) {
        if (this.urlExtractor == null) {
            validationErrors.add(new ConfigurationException("No urlExtractor defined"));
        }
    }
}
