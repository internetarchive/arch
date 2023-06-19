import{i as t,_ as e,a}from"./chunk-styles-690ba0e4.js";import{A as s}from"./chunk-arch-data-table-06b40996.js";import{P as i}from"./chunk-types-df9f20ed.js";import{P as l}from"./chunk-helpers-6405a525.js";import"./chunk-arch-loading-indicator-02b42d35.js";import"./arch-sub-collection-builder-2c0fad15.js";import"./chunk-arch-alert-46aea875.js";import"./arch-generate-dataset-form-e492768c.js";var d=[t`
    data-table {
      min-width: 60rem;
    }

    data-table > table {
      table-layout: fixed;
    }

    data-table > table > thead > tr > th.category {
      width: 6em;
    }

    data-table > table > thead > tr > th.sample {
      width: 4em;
    }

    data-table > table > thead > tr > th.state {
      width: 5em;
    }

    data-table > table > thead > tr > th.started {
      width: 9em;
    }

    data-table > table > thead > tr > th.finished {
      width: 9em;
    }

    data-table > table > thead > tr > th.files {
      width: 3em;
    }

    data-table > table > thead > tr > th {
      max-width: none;
    }
  `];let r=class extends s{willUpdate(t){super.willUpdate(t),this.apiCollectionEndpoint="/datasets",this.apiItemResponseIsArray=!0,this.apiItemTemplate="/datasets?collectionId=:collectionId&job=:jobId&sample=:isSample",this.itemPollPredicate=t=>t.state===i.Running,this.itemPollPeriodSeconds=3,this.apiStaticParamPairs=[["state!",i.NotStarted]],this.cellRenderers=[(t,e)=>e.state!==i.Finished?`${e.name}`:`<a href="${l.dataset(e.id,e.sample)}">${e.name}</a>`,void 0,(t,e)=>`<a href="${l.collection(e.collectionId)}">${t}</a>`,t=>-1===t?"No":"Yes",void 0,t=>null==t?void 0:t.slice(0,-3),(t,e)=>e.state===i.Running?"":null==t?void 0:t.slice(0,-3)],this.columns=["name","category","collectionName","sample","state","startTime","finishedTime","numFiles"],this.columnHeaders=["Dataset","Category","Collection","Sample","State","Started","Finished","Files"],this.singleName="Dataset",this.pluralName="Datasets"}};r.styles=[...s.styles,...d],r=e([a("arch-dataset-explorer-table")],r);export{r as ArchDatasetExplorerTable};
//# sourceMappingURL=arch-dataset-explorer-table-8ea8d1d8.js.map
