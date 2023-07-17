import{i as t,_ as e,e as a,a as i}from"./chunk-styles-75502ec5.js";import{A as l}from"./chunk-arch-data-table-41756688.js";import{T as s}from"./chunk-pubsub-5f508603.js";import{P as o}from"./chunk-helpers-799bde6a.js";import{P as d}from"./chunk-arch-generate-dataset-form-8e4f842f.js";import"./chunk-arch-loading-indicator-37c0007d.js";import"./arch-sub-collection-builder-5c9f5725.js";import"./chunk-arch-alert-384569c4.js";import"./chunk-query-all-273a2103.js";var n=[t`
    data-table > .paginator-wrapper {
      display: none;
    }

    data-table > table {
      table-layout: fixed;
    }

    data-table > table > thead > tr > th.category {
      width: 7em;
    }

    data-table > table > thead > tr > th.sample {
      width: 4em;
    }

    data-table > table > thead > tr > th.state {
      width: 7em;
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
  `];let r=class extends l{willUpdate(t){super.willUpdate(t),this.apiCollectionEndpoint="/datasets",this.apiItemResponseIsArray=!0,this.apiItemTemplate="/datasets?collectionId=:collectionId&job=:jobId&sample=:isSample",this.itemPollPredicate=t=>t.state===d.Running,this.itemPollPeriodSeconds=3,this.apiStaticParamPairs=[["collectionId",this.collectionId],["state!",d.NotStarted]],this.cellRenderers=[(t,e)=>e.state!==d.Finished?`${t}`:`<a href="${o.dataset(e.id,e.sample)}">${t}</a>`,void 0,t=>-1===t?"No":"Yes",void 0,t=>null==t?void 0:t.slice(0,-3),(t,e)=>e.state===d.Running?"":null==t?void 0:t.slice(0,-3)],this.columns=["name","category","sample","state","startTime","finishedTime","numFiles"],this.columnHeaders=["Name","Category","Sample","State","Started","Finished","Files"],this.nonSelectionActionLabels=["Generate a New Dataset"],this.nonSelectionActions=[s.GENERATE_DATASET],this.singleName="Dataset",this.pluralName="Datasets"}nonSelectionActionHandler(t){if(t===s.GENERATE_DATASET)window.location.href=o.generateCollectionDataset(this.collectionId)}};r.styles=[...l.styles,...n],e([a({type:String})],r.prototype,"collectionId",void 0),r=e([i("arch-collection-details-dataset-table")],r);export{r as ArchCollectionDetailsDatasetTable};
//# sourceMappingURL=arch-collection-details-dataset-table-5256b3e1.js.map
