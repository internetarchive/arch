import{i as t,_ as e,e as a,a as i}from"./chunk-styles-75502ec5.js";import{A as s}from"./chunk-arch-data-table-815c3971.js";import{T as l}from"./chunk-pubsub-5f508603.js";import{P as o}from"./chunk-helpers-94bb8932.js";import{P as d}from"./chunk-arch-generate-dataset-form-6a3e9363.js";import"./chunk-arch-loading-indicator-37c0007d.js";import"./arch-sub-collection-builder-cd409a8e.js";import"./chunk-arch-alert-a83c3a9d.js";import"./chunk-query-all-273a2103.js";var n=[t`
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
      width: 6em;
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
  `];let r=class extends s{willUpdate(t){super.willUpdate(t),this.apiCollectionEndpoint="/datasets",this.apiItemResponseIsArray=!0,this.apiItemTemplate="/datasets?collectionId=:collectionId&job=:jobId&sample=:isSample",this.itemPollPredicate=t=>t.state===d.Running,this.itemPollPeriodSeconds=3,this.apiStaticParamPairs=[["collectionId",this.collectionId],["state!",d.NotStarted]],this.cellRenderers=[(t,e)=>e.state!==d.Finished?`${t}`:`<a href="${o.dataset(e.id,e.sample)}">${t}</a>`,void 0,t=>-1===t?"No":"Yes",void 0,t=>null==t?void 0:t.slice(0,-3),(t,e)=>e.state===d.Running?"":null==t?void 0:t.slice(0,-3)],this.columnFilterDisplayMaps=[void 0,void 0,{100:"Yes",[-1]:"No"}],this.columns=["name","category","sample","state","startTime","finishedTime","numFiles"],this.columnHeaders=["Name","Category","Sample","State","Started","Finished","Files"],this.filterableColumns=[!0,!0,!0,!0,!1,!1,!1],this.nonSelectionActionLabels=["Generate a New Dataset"],this.nonSelectionActions=[l.GENERATE_DATASET],this.singleName="Dataset",this.sort="-startTime",this.sortableColumns=[!0,!0,!1,!0,!0,!0,!0],this.pluralName="Datasets"}nonSelectionActionHandler(t){if(t===l.GENERATE_DATASET)window.location.href=o.generateCollectionDataset(this.collectionId)}};r.styles=[...s.styles,...n],e([a({type:String})],r.prototype,"collectionId",void 0),r=e([i("arch-collection-details-dataset-table")],r);export{r as ArchCollectionDetailsDatasetTable};
//# sourceMappingURL=arch-collection-details-dataset-table-059708a7.js.map
