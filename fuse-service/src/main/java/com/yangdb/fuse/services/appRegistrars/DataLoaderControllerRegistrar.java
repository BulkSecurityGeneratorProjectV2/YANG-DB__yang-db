package com.yangdb.fuse.services.appRegistrars;

/*-
 * #%L
 * fuse-service
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

/*-
 *
 * fuse-service
 * %%
 * Copyright (C) 2016 - 2019 yangdb   ------ www.yangdb.org ------
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
 *
 */

import com.yangdb.fuse.dispatcher.urlSupplier.AppUrlSupplier;
import com.yangdb.fuse.executor.ontology.schema.load.GraphDataLoader;
import com.yangdb.fuse.model.logical.LogicalGraphModel;
import com.yangdb.fuse.services.controllers.DataLoaderController;
import org.jooby.Jooby;
import org.jooby.Results;
import org.jooby.Upload;

import java.io.File;

public class DataLoaderControllerRegistrar extends AppControllerRegistrarBase<DataLoaderController> {
    //region Constructors
    public DataLoaderControllerRegistrar() {
        super(DataLoaderController.class);
    }
    //endregion

    //region AppControllerRegistrarBase Implementation
    @Override
    public void register(Jooby app, AppUrlSupplier appUrlSupplier) {
        // create mapping according to ontology & given indexProvider instructions
        app.get("/fuse/load/ontology/:id/mapping",
                req -> Results.with(this.getController(app)
                        .createMapping(req.param("id").value())));
        // create indices according to ontology & given indexProvider instructions
        app.get("/fuse/load/ontology/:id/indices",
                req -> Results.with(this.getController(app)
                        .createIndices(req.param("id").value())));

        // Initiate Graph indices
        app.get("/fuse/load/ontology/:id/init",
                req -> Results.with(this.getController(app).init(req.param("id").value())));
        // drop Graph indices
        app.get("/fuse/load/ontology/:id/drop",
                req -> Results.with(this.getController(app).drop(req.param("id").value())));

        // Upload string data in json graph format
        app.post("/fuse/load/ontology/:id/graph/load",
                req -> Results.json(this.getController(app)
                        .loadGraph(req.param("id").value(), req.body(LogicalGraphModel.class),
                                req.param("directive").isSet() ?
                                        GraphDataLoader.Directive.valueOf(req.param("directive").value().toUpperCase()) : GraphDataLoader.Directive.INSERT )));

        // Upload string data in csv format
       app.post("/fuse/load/ontology/:id/csv/load",
                req -> Results.json(this.getController(app)
                        .loadCsv(req.param("id").value(),
                                req.param("type").value(),
                                req.param("label").value(),
                                req.body(String.class), req.param("directive").isSet() ?
                                GraphDataLoader.Directive.valueOf(req.param("directive").value().toUpperCase()) : GraphDataLoader.Directive.INSERT)));

        // Upload file data in json format
        app.post("/fuse/load/ontology/:id/graph/upload",
                req -> {
                    Upload upload = req.file("file");
                    try {
                        //todo check file type -> process zipped file
                        File file = upload.file();
                        return Results.json(this.getController(app)
                                .loadGraph(req.param("id").value(), file,
                                        req.param("directive").isSet() ?
                                                GraphDataLoader.Directive.valueOf(req.param("directive").value().toUpperCase()) : GraphDataLoader.Directive.INSERT ));
                    } finally {
                        upload.close();
                    }
                });

        // Upload file data in csv format
        app.post("/fuse/load/ontology/:id/csv/upload",
                req -> {
                    Upload upload = req.file("file");
                    try {
                        //todo check file type -> process zipped file
                        File file = upload.file();
                        return Results.json(this.getController(app)
                                .loadCsv(req.param("id").value(),
                                        req.param("type").value(),
                                        req.param("label").value(),
                                        file, req.param("directive").isSet() ?
                                                GraphDataLoader.Directive.valueOf(req.param("directive").value().toUpperCase()) : GraphDataLoader.Directive.INSERT));
                    } finally {
                        upload.close();
                    }
                });
    }
    //endregion
}
