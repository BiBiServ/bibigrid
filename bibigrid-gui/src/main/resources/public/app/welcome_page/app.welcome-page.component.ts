import {Component, EventEmitter, Output} from "@angular/core";

@Component({
    selector: 'welcome',
    template: `

<nav class="navbar navbar-default navbar-fixed-top">
  <div class="container-fluid">
    <div class="navbar-header">
      <button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#myNavbar">
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
      </button>
      <a class="navbar-brand" href="#">BiBiGrid</a>
    </div>
    <div class="collapse navbar-collapse" id="myNavbar">
      <ul class="nav navbar-nav navbar-right">
        <li><a href="#home">HOME</a></li>
        <li><a href="#"><span class="glyphicon glyphicon-search"></span></a></li>
      </ul>
    </div>
  </div>
</nav>

    <div id="myCarousel" class="carousel slide" data-ride="carousel">
    
    <div class="carousel-inner" role="listbox">
      <div class="item active">
        <img src="./pictures/cloud-computing.jpg" alt="Cloud Computing" width="1200" height="700">
        <div class="carousel-caption">
          <h3>BiBiGrid</h3>
          <p>Cloud based cluster setups made easy</p>
        </div>      
      </div>
    </div>
    </div>

    <div class="container text-center">
  <br>
  <br>
  <h3>Welcome to the BiBiGrid Website</h3>
  <p><small>Before you can start to use the BiBiGrid, lets quickly do some initial set up.
  If you answer me one simple question I will pre-configure the BiBiGrid website, to take as much work as possible away from you.
  If you improve later on or are unhappy with my pre-configuration, just get back to this page and we can do it differenly. </small></p>
  <br>
  <br>
  <br>
  <div class="row">
    <div class="col-sm-4">
      <p class="text-center"><strong>Beginner</strong></p><br>
      <a href="#demo" data-toggle="collapse">
      <img src="./pictures/beginner.jpg" class="img-circle person" alt="Random Name" width="255" height="255">
      </a>
      <div id="demo" class="collapse">
        <p>"BiBiGrid?!? What's that? It sounds yummy, can I eat it?"</p>
        <p>If these or similar toughts cross your mind this choice is the right one for you.</p>
        <p>In the beginner mode all choices are made, leaving only the bare minimum for you. Basically you just have to hit the run button.</p>
        <button class="btn pull-center" type="submit" (click)="chooseMode('beginner')">Let's start</button>
      </div>
    </div>
    <div class="col-sm-4">
      <p><strong>Intermediate</strong></p><br>
      <a href="#demo2" data-toggle="collapse">
      <img src="./pictures/intermediate.jpg" class="img-circle person" alt="Random Name" width="255" height="255">
    </a>
      <div id="demo2" class="collapse">
        <p>"Stop insulting me, I know what I am doing. I am not a baby any more!"</p>
        <p>You used the BiBiGrid bevor and you are familiar with the basic flags & functions ? Then this mode is the right one for you.</p>
        <p>In the intermediate mode you have access to much more options to further tweak your grid. </p>
        <button class="btn pull-center" type="submit" (click)="chooseMode('intermediate')">Let's start</button>
      </div>
    </div>
    <div class="col-sm-4">
      <p><strong>Expert</strong></p><br>
      <a href="#demo3" data-toggle="collapse">
      <img src="./pictures/expert.png" class="img-circle person" alt="Random Name" width="255" height="255">
     </a>
      <div id="demo3" class="collapse">
        <p>"This site is totaly buggy. I could do a whole lot better, if I hade the time!"</p>
        <p>You could have written the BiBiGrid yourself and already know all there is to know about it? Then pick this mode.</p>
        <p>The expert mode gives you access to all available options, for the full BiBiGrid experience.</p>
        <button class="btn pull-center" type="submit" (click)="chooseMode('expert')">Let's start</button>
      </div>
    </div>
  </div>
</div>

<footer class="text-center">
  <a class="up-arrow" href="#myCarousel" data-toggle="tooltip" title="TO TOP">
    <span class="glyphicon glyphicon-chevron-up"></span>
  </a><br><br>
  University of Bielefeld (2017)
</footer>
`,
    providers:[]
})

export class welcomePage {

    @Output() notify: EventEmitter<string> = new EventEmitter<string>();

    constructor() {
    }

    chooseMode(choice: string) {
        this.notify.emit(choice);
    }

}