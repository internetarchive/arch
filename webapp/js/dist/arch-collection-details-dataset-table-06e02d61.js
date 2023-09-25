import{i as t,_ as e,e as a,a as i}from"./chunk-styles-48eb3c3c.js";import{t as s}from"./chunk-state-b70c226d.js";import{A as l}from"./chunk-arch-data-table-c93bbcda.js";import{T as o}from"./chunk-pubsub-5f508603.js";import{P as r}from"./chunk-helpers-7d3afcd3.js";import{P as d}from"./chunk-arch-generate-dataset-form-a4fc78d3.js";import"./chunk-arch-loading-indicator-93cca752.js";import"./arch-hover-tooltip-826aa900.js";import"./chunk-scale-large-c2ff54fd.js";import"./arch-sub-collection-builder-b75d93c9.js";import"./chunk-arch-alert-84f9ff63.js";import"./chunk-query-all-ac9cba43.js";var n=[t`
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
  `];let c=class extends l{constructor(){super(...arguments),this.columnNameHeaderTooltipMap={category:"Dataset categories are Collection, Network, Text, and File Format",sample:"Sample datasets contain only the first 100 available records from a collection"}}willUpdate(t){super.willUpdate(t),this.apiCollectionEndpoint="/datasets",this.apiItemResponseIsArray=!0,this.apiItemTemplate="/datasets?collectionId=:collectionId&job=:jobId&sample=:isSample",this.itemPollPredicate=t=>t.state===d.Running,this.itemPollPeriodSeconds=3,this.apiStaticParamPairs=[["collectionId",this.collectionId],["state!",d.NotStarted]],this.cellRenderers=[(t,e)=>e.state!==d.Finished?`${t}`:`<a href="${r.dataset(e.id,e.sample)}">${t}</a>`,void 0,t=>-1===t?"No":"Yes",void 0,t=>null==t?void 0:t.slice(0,-3),(t,e)=>e.state===d.Running?"":null==t?void 0:t.slice(0,-3)],this.columnFilterDisplayMaps=[void 0,void 0,{100:"Yes",[-1]:"No"}],this.columns=["name","category","sample","state","startTime","finishedTime","numFiles"],this.columnHeaders=["Dataset","Category","Sample","State","Started","Finished","Files"],this.filterableColumns=[!0,!0,!0,!0,!1,!1,!1],this.nonSelectionActionLabels=["Generate a New Dataset"],this.nonSelectionActions=[o.GENERATE_DATASET],this.singleName="Dataset",this.sort="-startTime",this.sortableColumns=[!0,!0,!0,!0,!0,!0,!0],this.pluralName="Datasets"}nonSelectionActionHandler(t){if(t===o.GENERATE_DATASET)window.location.href=r.generateCollectionDataset(this.collectionId)}};c.styles=[...l.styles,...n],e([a({type:String})],c.prototype,"collectionId",void 0),e([s()],c.prototype,"columnNameHeaderTooltipMap",void 0),c=e([i("arch-collection-details-dataset-table")],c);export{c as ArchCollectionDetailsDatasetTable};
//# sourceMappingURL=arch-collection-details-dataset-table-06e02d61.js.map
