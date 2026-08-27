/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.grails.intellij.plugin.structure;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.util.version.Version;

/**
 * Fully qualified names of Grails/GORM classes whose packages moved between major versions.
 * The prefix and the constraint sub-package differ per era, so callers resolve an instance for
 * the context's application rather than hardcoding names.
 */
public final class GrailsCommonClassNames {

  private static final String HIBERNATE_MAPPING_BUILDER = "orm.hibernate.cfg.HibernateMappingBuilder";
  private static final String CREDIT_CARD_CONSTRAINT = "CreditCardConstraint";
  private static final String MAX_SIZE_CONSTRAINT = "MaxSizeConstraint";
  private static final String EMAIL_CONSTRAINT = "EmailConstraint";
  private static final String BLANK_CONSTRAINT = "BlankConstraint";
  private static final String RANGE_CONSTRAINT = "RangeConstraint";
  private static final String URL_CONSTRAINT = "UrlConstraint";
  private static final String SIZE_CONSTRAINT = "SizeConstraint";
  private static final String IN_LIST_CONSTRAINT = "InListConstraint";
  private static final String MATCHES_CONSTRAINT = "MatchesConstraint";
  private static final String MIN_CONSTRAINT = "MinConstraint";
  private static final String MAX_CONSTRAINT = "MaxConstraint";
  private static final String MIN_SIZE_CONSTRAINT = "MinSizeConstraint";
  private static final String SCALE_CONSTRAINT = "ScaleConstraint";
  private static final String NOT_EQUAL_CONSTRAINT = "NotEqualConstraint";
  private static final String NULLABLE_CONSTRAINT = "NullableConstraint";
  private static final String VALIDATOR_CONSTRAINT = "ValidatorConstraint";
  private static final String UNIQUE_CONSTRAINT = "UniqueConstraint";
  private static final String URL_MAPPING_BUILDER = "web.mapping.DefaultUrlMappingEvaluator.UrlMappingBuilder";

  private static final GrailsCommonClassNames AFTER_40 =
    new GrailsCommonClassNames("org.grails.", "datastore.gorm.validation.constraints.");
  private static final GrailsCommonClassNames AFTER_30 =
    new GrailsCommonClassNames("org.grails.", "validation.");
  private static final GrailsCommonClassNames BEFORE_30 =
    new GrailsCommonClassNames("org.codehaus.groovy.grails.", "validation.");

  private final String hibernateMappingBuilder;
  private final String creditCardConstraint;
  private final String emailConstraint;
  private final String blankConstraint;
  private final String rangeConstraint;
  private final String inListConstraint;
  private final String urlConstraint;
  private final String sizeConstraint;
  private final String matchesConstraint;
  private final String minConstraint;
  private final String maxConstraint;
  private final String minSizeConstraint;
  private final String maxSizeConstraint;
  private final String scaleConstraint;
  private final String notEqualConstraint;
  private final String nullableConstraint;
  private final String validatorConstraint;
  private final String uniqueConstraint;
  private final String urlMappingBuilder;

  private GrailsCommonClassNames(@NotNull String globalPrefix, @NotNull String constraintPath) {
    hibernateMappingBuilder = globalPrefix + HIBERNATE_MAPPING_BUILDER;
    creditCardConstraint = globalPrefix + constraintPath + CREDIT_CARD_CONSTRAINT;
    emailConstraint = globalPrefix + constraintPath + EMAIL_CONSTRAINT;
    blankConstraint = globalPrefix + constraintPath + BLANK_CONSTRAINT;
    rangeConstraint = globalPrefix + constraintPath + RANGE_CONSTRAINT;
    inListConstraint = globalPrefix + constraintPath + IN_LIST_CONSTRAINT;
    urlConstraint = globalPrefix + constraintPath + URL_CONSTRAINT;
    sizeConstraint = globalPrefix + constraintPath + SIZE_CONSTRAINT;
    matchesConstraint = globalPrefix + constraintPath + MATCHES_CONSTRAINT;
    minConstraint = globalPrefix + constraintPath + MIN_CONSTRAINT;
    maxConstraint = globalPrefix + constraintPath + MAX_CONSTRAINT;
    minSizeConstraint = globalPrefix + constraintPath + MIN_SIZE_CONSTRAINT;
    maxSizeConstraint = globalPrefix + constraintPath + MAX_SIZE_CONSTRAINT;
    scaleConstraint = globalPrefix + constraintPath + SCALE_CONSTRAINT;
    notEqualConstraint = globalPrefix + constraintPath + NOT_EQUAL_CONSTRAINT;
    nullableConstraint = globalPrefix + constraintPath + NULLABLE_CONSTRAINT;
    validatorConstraint = globalPrefix + constraintPath + VALIDATOR_CONSTRAINT;
    uniqueConstraint = globalPrefix + constraintPath + UNIQUE_CONSTRAINT;
    urlMappingBuilder = globalPrefix + URL_MAPPING_BUILDER;
  }

  public static @NotNull GrailsCommonClassNames getInstance(@NotNull PsiElement context) {
    GrailsApplication app = GrailsApplicationManager.findApplication(context);
    if (app == null) return BEFORE_30;
    if (Version.AT_LEAST_4.contains(app.getGrailsVersion())) return AFTER_40;
    if (Version.AT_LEAST_3.contains(app.getGrailsVersion())) return AFTER_30;
    return BEFORE_30;
  }

  public @NotNull String getHibernateMappingBuilder() { return hibernateMappingBuilder; }

  public @NotNull String getCreditCardConstraint() { return creditCardConstraint; }

  public @NotNull String getEmailConstraint() { return emailConstraint; }

  public @NotNull String getBlankConstraint() { return blankConstraint; }

  public @NotNull String getRangeConstraint() { return rangeConstraint; }

  public @NotNull String getInListConstraint() { return inListConstraint; }

  public @NotNull String getUrlConstraint() { return urlConstraint; }

  public @NotNull String getSizeConstraint() { return sizeConstraint; }

  public @NotNull String getMatchesConstraint() { return matchesConstraint; }

  public @NotNull String getMinConstraint() { return minConstraint; }

  public @NotNull String getMaxConstraint() { return maxConstraint; }

  public @NotNull String getMinSizeConstraint() { return minSizeConstraint; }

  public @NotNull String getMaxSizeConstraint() { return maxSizeConstraint; }

  public @NotNull String getScaleConstraint() { return scaleConstraint; }

  public @NotNull String getNotEqualConstraint() { return notEqualConstraint; }

  public @NotNull String getNullableConstraint() { return nullableConstraint; }

  public @NotNull String getValidatorConstraint() { return validatorConstraint; }

  public @NotNull String getUniqueConstraint() { return uniqueConstraint; }

  public @NotNull String getUrlMappingBuilder() { return urlMappingBuilder; }
}
