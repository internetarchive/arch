import{i as t,_ as e,a}from"./chunk-styles-4a7b21cd.js";import{A as l}from"./chunk-arch-data-table-27d195d8.js";import{P as s}from"./chunk-arch-generate-dataset-form-4e7f3f51.js";import{P as i}from"./chunk-helpers-d7a83325.js";import"./chunk-arch-loading-indicator-044bc257.js";import"./chunk-arch-alert-3c1ceea9.js";import"./chunk-query-all-2f0be97e.js";import"./arch-sub-collection-builder-ff752dbf.js";var d=[t`
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
  `];let r=class extends l{willUpdate(t){super.willUpdate(t),this.apiCollectionEndpoint="/datasets",this.apiItemResponseIsArray=!0,this.apiItemTemplate="/datasets?collectionId=:collectionId&job=:jobId&sample=:isSample",this.itemPollPredicate=t=>t.state===s.Running,this.itemPollPeriodSeconds=3,this.apiStaticParamPairs=[["state!",s.NotStarted]],this.cellRenderers=[(t,e)=>e.state!==s.Finished?`${e.name}`:`<a href="${i.dataset(e.id,e.sample)}">${e.name}</a>`,void 0,(t,e)=>`<a href="${i.collection(e.collectionId)}">${t}</a>`,t=>-1===t?"No":"Yes",void 0,t=>null==t?void 0:t.slice(0,-3),(t,e)=>e.state===s.Running?"":null==t?void 0:t.slice(0,-3)],this.columns=["name","category","collectionName","sample","state","startTime","finishedTime","numFiles"],this.columnHeaders=["Dataset","Category","Collection","Sample","State","Started","Finished","Files"],this.singleName="Dataset",this.pluralName="Datasets"}};r.styles=[...l.styles,...d],r=e([a("arch-dataset-explorer-table")],r);export{r as ArchDatasetExplorerTable};
//# sourceMappingURL=arch-dataset-explorer-table-ea80fe61.js.map
