package com.yangdb.fuse.generator.model.relation;

/*-
 * #%L
 * fuse-domain-dragons-datagen
 * %%
 * Copyright (C) 2016 - 2019 The YangDb Graph Database Project
 * %%
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
 * #L%
 */



import com.yangdb.fuse.generator.model.enums.EntityType;
import com.yangdb.fuse.generator.model.enums.RelationType;

import java.util.Date;

/**
 * Created by benishue on 05-Jun-17.
 */
public class Originated extends RelationBase {

    //region Ctrs
    public Originated(String id, EntityType entityType, String source, String target, Date since) {
        super(id, source, target, RelationType.ORIGINATED);
        this.entityType = entityType;
        this.since = since;
    }
    //endregion

    //region Getters & Setters
    public Date getSince() {
        return since;
    }

    public void setSince(Date since) {
        this.since = since;
    }
    //endregion

    //region Public Methods
    @Override
    public String[] getRecord() {
        return new String[]{
                this.getId(),
                this.getSource(),
                entityType.name(),// source entity type
                this.getTarget(),
                "Kingdom",// target entity type
                Long.toString(this.getSince().getTime())
        };
    }
    //endregion

    private EntityType entityType;
    //region Fields
    private Date since;
    //endregion
}

