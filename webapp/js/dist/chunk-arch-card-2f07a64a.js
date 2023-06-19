import{g as t,i as e,_ as r,e as s,s as i,y as o,a as h}from"./chunk-styles-690ba0e4.js";var a=[t,e`
    :host {
      display: block;
      background-color: #fff;
      padding: 1rem 1rem 3rem 1rem;
      height: calc(100% - 4rem);
      box-shadow: 1px 1px 6px #888;
      font-size: 0.95rem;
      border-radius: 6px;
      position: relative;
    }

    :host .header {
      display: flex;
    }

    :host .header > *:first-child {
      flex-grow: 1;
    }

    :host .header > a {
      margin: auto;
    }

    :host .footer {
      position: absolute;
      left: 0;
      right: 0;
      bottom: 1rem;
      text-align: center;
    }
  `];let l=class extends i{constructor(){super(...arguments),this.title="Title",this.headerLevel=2,this.ctaText=void 0,this.ctaHref=void 0}get header(){switch(this.headerLevel){case 1:return o`<h1>${this.title}</h1>`;case 2:return o`<h2>${this.title}</h2>`;case 3:return o`<h3>${this.title}</h3>`;case 4:return o`<h4>${this.title}</h4>`;case 5:return o`<h5>${this.title}</h5>`;default:return o`<h6>${this.title}</h6>`}}render(){return o`
      <section>
        <div class="header">
          ${this.header}
          ${this.ctaText&&this.ctaHref?o`<a href="${this.ctaHref}">${this.ctaText}</a>`:""}
        </div>
        <hr />
        <slot name="content"></slot>
        <div class="footer">
          <slot name="footer"></slot>
        </div>
      </section>
    `}};l.styles=a,r([s({type:String})],l.prototype,"title",void 0),r([s({type:Number})],l.prototype,"headerLevel",void 0),r([s({type:String})],l.prototype,"ctaText",void 0),r([s({type:String})],l.prototype,"ctaHref",void 0),l=r([h("arch-card")],l);
//# sourceMappingURL=chunk-arch-card-2f07a64a.js.map
