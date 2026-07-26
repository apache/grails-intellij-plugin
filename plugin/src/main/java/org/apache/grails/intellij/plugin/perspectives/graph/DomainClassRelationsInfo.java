/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors.
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
 */

package org.apache.grails.intellij.plugin.perspectives.graph;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.GrailsBundle;

public class DomainClassRelationsInfo {
//  @Nullable
//  public String getVarNameBackward() {
//    return myVarNameBackward;
//  }
//
//  public void setVarNameBackward(@Nullable String varNameBackward) {
//    myVarNameBackward = varNameBackward;
//  }

  public enum Relation {
    UNKNOWN,
    BELONGS_TO,
    HAS_MANY,
    STRONG,
    DOUBLESTRONG,
    MANY_TO_MANY
  }

  public static final String UNKNOWN_NAME = "unknown";
  public static final String BELONGS_TO_NAME = "belongsTo";
  public static final String HAS_MANY_NAME = "hasMany";
  public static final String RELATES_TO_MANY_NAME = "relatesToMany";
  public static final String HAS_ONE_NAME = "hasOne";
  public static final String TRANSIENTS_NAME = "transients";
  public static final String CONSTRAINTS_NAME = "constraints";
  public static final String MAPPED_BY = "mappedBy";

  private final DomainClassNode mySource;
  private final DomainClassNode myTarget;
  private Relation myRelation;

  private @Nullable String myVarName;

//  @Nullable
//  private String myVarNameBackward;

  public DomainClassRelationsInfo(DomainClassNode source, DomainClassNode target, Relation relation) {
    mySource = source;
    myTarget = target;
    myRelation = relation;
  }

  public @NotNull DomainClassNode getSource() {
    return mySource;
  }

  public @NotNull DomainClassNode getTarget() {
    return myTarget;
  }

  public Relation getRelation() {
    return myRelation;
  }

  public void setRelation(Relation relation) {
    myRelation = relation;
  }

  public @NotNull String getEdgeLabel() {
    return switch (myRelation) {
      case UNKNOWN, DOUBLESTRONG -> GrailsBundle.message("domain.classes.relations.strong.strong");
      case STRONG -> GrailsBundle.message("domain.classes.relations.strong");
      case BELONGS_TO -> GrailsBundle.message("domain.classes.relations.belongs.to");
      case HAS_MANY -> GrailsBundle.message("domain.classes.relations.has.many");
      case MANY_TO_MANY -> GrailsBundle.message("domain.classes.relations.has.many.to.many");
    };
  }

  public void setVarName(@Nullable String varName) {
    this.myVarName = varName;
  }

  @Override
  public boolean equals(final Object otherDomainClssRelatiionInfo) {
    if (this == otherDomainClssRelatiionInfo) return true;
    if (otherDomainClssRelatiionInfo == null || getClass() != otherDomainClssRelatiionInfo.getClass()) return false;

    final DomainClassRelationsInfo that = (DomainClassRelationsInfo) otherDomainClssRelatiionInfo;

    if (myRelation != that.getRelation()) return false;
    if (!mySource.equals(that.mySource)) return false;
    if (!myTarget.equals(that.myTarget)) return false;
    if ((myVarName != null) && !myVarName.equals(that.myVarName)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result;
    result = mySource.hashCode();
    result = 31 * result + myTarget.hashCode();
    result = 31 * result + myRelation.hashCode();
    return result;
  }

  public @Nullable String getVarName() {
    return myVarName;
  }
}