/*
 * Copyright 2020 Flipkart Internet, pvt ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flipkart.gojira.requestsampling;

import com.flipkart.gojira.core.Mode;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Interface that exposes sampling related configuration.
 */
public abstract class RequestSamplingRepository {

  protected double samplingPercentage = 0.00d;
  protected List<Pattern> whitelist = null;

  /**
   * Returns the {@link #samplingPercentage} set in {@link RequestSamplingModule}.
   */
  public abstract double getSamplingPercentage();

  /**
   * This method sets the sampling percentage for {@link Mode#PROFILE} mode in {@link
   * RequestSamplingModule}.
   */
  abstract void setSamplingPercentage(double samplingPercentage);

  /**
   * Returns {@link #whitelist} set in {@link RequestSamplingModule}.
   */
  public abstract List<Pattern> getWhitelist();

  /**
   * This method sets the whiteList for {@link Mode#PROFILE} mode in {@link RequestSamplingModule}
   * in {@link #whitelist}.
   */
  abstract void setWhitelist(List<Pattern> whitelist);
}
