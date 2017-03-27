"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
Object.defineProperty(exports, "__esModule", { value: true });
var core_1 = require("@angular/core");
var welcomePage = (function () {
    function welcomePage() {
        this.notify = new core_1.EventEmitter();
    }
    welcomePage.prototype.chooseMode = function (choice) {
        this.notify.emit(choice);
    };
    return welcomePage;
}());
__decorate([
    core_1.Output(),
    __metadata("design:type", core_1.EventEmitter)
], welcomePage.prototype, "notify", void 0);
welcomePage = __decorate([
    core_1.Component({
        selector: 'welcome',
        template: "\n\n<nav class=\"navbar navbar-default navbar-fixed-top\">\n  <div class=\"container-fluid\">\n    <div class=\"navbar-header\">\n      <button type=\"button\" class=\"navbar-toggle\" data-toggle=\"collapse\" data-target=\"#myNavbar\">\n        <span class=\"icon-bar\"></span>\n        <span class=\"icon-bar\"></span>\n        <span class=\"icon-bar\"></span>\n      </button>\n      <a class=\"navbar-brand\" href=\"#\">BiBiGrid</a>\n    </div>\n    <div class=\"collapse navbar-collapse\" id=\"myNavbar\">\n      <ul class=\"nav navbar-nav navbar-right\">\n        <li><a href=\"#home\">HOME</a></li>\n        <li><a href=\"#\"><span class=\"glyphicon glyphicon-search\"></span></a></li>\n      </ul>\n    </div>\n  </div>\n</nav>\n\n    <div id=\"myCarousel\" class=\"carousel slide\" data-ride=\"carousel\">\n    \n    <div class=\"carousel-inner\" role=\"listbox\">\n      <div class=\"item active\">\n        <img src=\"./pictures/cloud-computing.jpg\" alt=\"Cloud Computing\" width=\"1200\" height=\"700\">\n        <div class=\"carousel-caption\">\n          <h3>BiBiGrid</h3>\n          <p>Cloud based cluster setups made easy</p>\n        </div>      \n      </div>\n    </div>\n    </div>\n\n    <div class=\"container text-center\">\n  <br>\n  <br>\n  <h3>Welcome to the BiBiGrid Website</h3>\n  <p><small>Before you can start to use the BiBiGrid, lets quickly do some initial setup.\n  If you answer one simple question I will pre-configure the BiBiGrid website, to take as much work as possible away from you.\n  If you improve later on or are unhappy with the pre-configuration, just get back to this page and we can do it differently. </small></p>\n  <br>\n  <br>\n  <br>\n  <div class=\"row\">\n    <div class=\"col-sm-4\">\n      <p class=\"text-center\"><strong>Beginner</strong></p><br>\n      <a href=\"#beginner\" data-toggle=\"collapse\">\n      <img src=\"./pictures/beginner.jpg\" class=\"img-circle person\" alt=\"Random Name\" width=\"255\" height=\"255\">\n      </a>\n      <div id=\"beginner\" class=\"collapse\">\n        <p>\"BiBiGrid?!? What's that?\"</p>\n        <p>If these or similar thoughts cross your mind this choice is the right one for you.</p>\n        <p>In the beginner mode all choices are made, leaving only the bare minimum for you. Basically, you just have to hit the run button.</p>\n        <button class=\"btn pull-center\" type=\"submit\" (click)=\"chooseMode('beginner')\">Let's start</button>\n      </div>\n    </div>\n    <div class=\"col-sm-4\">\n      <p><strong>Intermediate</strong></p><br>\n      <a href=\"#intermediate\" data-toggle=\"collapse\">\n      <img src=\"./pictures/intermediate.jpg\" class=\"img-circle person\" alt=\"Random Name\" width=\"255\" height=\"255\">\n    </a>\n      <div id=\"intermediate\" class=\"collapse\">\n        <p>\"Stop insulting me, I know what I am doing. I am not a baby any more!\"</p>\n        <p>You used the BiBiGrid before and you are familiar with the basic flags & functions? Then this mode is the right one for you.</p>\n        <p>In the intermediate mode, you have access to much more options to further tweak your grid. </p>\n        <button class=\"btn pull-center\" type=\"submit\" (click)=\"chooseMode('intermediate')\">Let's start</button>\n      </div>\n    </div>\n    <div class=\"col-sm-4\">\n      <p><strong>Expert</strong></p><br>\n      <a href=\"#expert\" data-toggle=\"collapse\">\n      <img src=\"./pictures/expert.png\" class=\"img-circle person\" alt=\"Random Name\" width=\"255\" height=\"255\">\n     </a>\n      <div id=\"expert\" class=\"collapse\">\n        <p>\"This site is totally buggy. I could do a whole lot better, if I had the time!\"</p>\n        <p>You could have written the BiBiGrid yourself and already know all there is to know about it? Then pick this mode.</p>\n        <p>The expert mode gives you access to all available options, for the full BiBiGrid experience.</p>\n        <button class=\"btn pull-center\" type=\"submit\" (click)=\"chooseMode('expert')\">Let's start</button>\n      </div>\n    </div>\n  </div>\n</div>\n\n<footer class=\"text-center\">\n  <a class=\"up-arrow\" href=\"#myCarousel\" data-toggle=\"tooltip\" title=\"TO TOP\">\n    <span class=\"glyphicon glyphicon-chevron-up\"></span>\n  </a><br><br>\n  University of Bielefeld (2017)\n</footer>\n",
        providers: []
    }),
    __metadata("design:paramtypes", [])
], welcomePage);
exports.welcomePage = welcomePage;
//# sourceMappingURL=app.welcome-page.component.js.map