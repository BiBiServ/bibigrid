import {Component} from "@angular/core";

@Component({
    selector: 'navbar',
    template: `
<nav class="navbar navbar-inverse navbar-fixed-top">
      <div class="container">
        <div class="navbar-header">
          <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar" aria-expanded="false" aria-controls="navbar">
            <span class="sr-only">Toggle navigation</span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </button>
          <a class="navbar-brand" href="#"><span class="glyphicon glyphicon-cloud-upload" aria-hidden="true"></span> BiBiGrid-GUI</a>
        </div>
        <div id="navbar" class="collapse navbar-collapse">
          <ul class="nav navbar-nav">
            <li><a href="#"><span class="glyphicon glyphicon-home" aria-hidden="true"></span> Home</a></li>
            <li><a href="#submit"><span class="glyphicon glyphicon-send" aria-hidden="true"></span> Submit</a></li>
            <li><a href="#output"><span class="glyphicon glyphicon-bullhorn" aria-hidden="true"></span> Output</a></li>
            <li><a href="https://github.com/BiBiServ/bibigrid">Read more on Github</a></li>
          </ul>
        </div><!--/.nav-collapse -->
      </div>
    </nav>
`,
    providers:[]
})

export class BiBiGridGuiNavbar {}