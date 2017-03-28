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
var core_1 = require("@angular/core");
var BiBiGridGuiNavbar = (function () {
    function BiBiGridGuiNavbar() {
    }
    BiBiGridGuiNavbar = __decorate([
        core_1.Component({
            selector: 'navbar',
            template: "\n<nav class=\"navbar navbar-inverse navbar-fixed-top\">\n      <div class=\"container\">\n        <div class=\"navbar-header\">\n          <button type=\"button\" class=\"navbar-toggle collapsed\" data-toggle=\"collapse\" data-target=\"#navbar\" aria-expanded=\"false\" aria-controls=\"navbar\">\n            <span class=\"sr-only\">Toggle navigation</span>\n            <span class=\"icon-bar\"></span>\n            <span class=\"icon-bar\"></span>\n            <span class=\"icon-bar\"></span>\n          </button>\n          <a class=\"navbar-brand\" href=\"#\"><span class=\"glyphicon glyphicon-cloud-upload\" aria-hidden=\"true\"></span> BiBiGrid-GUI</a>\n        </div>\n        <div id=\"navbar\" class=\"collapse navbar-collapse\">\n          <ul class=\"nav navbar-nav\">\n            <li><a href=\"#\"><span class=\"glyphicon glyphicon-home\" aria-hidden=\"true\"></span> Home</a></li>\n            <li><a href=\"#submit\"><span class=\"glyphicon glyphicon-send\" aria-hidden=\"true\"></span> Submit</a></li>\n            <li><a href=\"#output\"><span class=\"glyphicon glyphicon-bullhorn\" aria-hidden=\"true\"></span> Output</a></li>\n            <li><a href=\"https://github.com/BiBiServ/bibigrid\">Read more on Github</a></li>\n          </ul>\n        </div><!--/.nav-collapse -->\n      </div>\n    </nav>\n",
            providers: []
        }), 
        __metadata('design:paramtypes', [])
    ], BiBiGridGuiNavbar);
    return BiBiGridGuiNavbar;
}());
exports.BiBiGridGuiNavbar = BiBiGridGuiNavbar;
//# sourceMappingURL=app.bibigrid-gui.navbar.js.map