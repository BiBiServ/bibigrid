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
var mainPage = (function () {
    function mainPage() {
    }
    return mainPage;
}());
mainPage = __decorate([
    core_1.Component({
        selector: 'mainpage',
        template: "\n   <nav class=\"navbar navbar-default navbar-fixed-top\">\n  <div class=\"container-fluid\">\n    <div class=\"navbar-header\">\n      <button type=\"button\" class=\"navbar-toggle\" data-toggle=\"collapse\" data-target=\"#myNavbar\">\n        <span class=\"icon-bar\"></span>\n        <span class=\"icon-bar\"></span>\n        <span class=\"icon-bar\"></span>\n      </button>\n      <a class=\"navbar-brand\" href=\"#\">BiBiGrid</a>\n    </div>\n    <div class=\"collapse navbar-collapse\" id=\"myNavbar\">\n      <ul class=\"nav navbar-nav navbar-right\">\n        <li><a href=\"#home\">HOME</a></li>\n        <li><a href=\"#\"><span class=\"glyphicon glyphicon-search\"></span></a></li>\n      </ul>\n    </div>\n  </div>\n</nav>\n<div class=\"container\">\n<table class=\"table table-inverse table-striped table-bordered table-hover\">\n<thead>\n      <tr>\n        <th>Short flag</th>\n        <th>Long flag</th>\n        <th>Description</th>\n        <th>Input</th>\n      </tr>\n    </thead>\n<tbody>\n<tr><th>M</th><th>master-image</th><th>machine image id for master</th><th><div class=\"input-group\"><input class=\"form-control\" type=\"text\"><span class=\"input-group-addon\" id=\"addon-type\">String</span></div></th></tr>\n<tr><th>v</th><th>verbose</th><th>more console output</th><th><input class=\"form-control\" type=\"checkbox\"></th></tr>\n<tr><th>psi</th><th>public-slave-ip</th><th>Slave instances also get an public ip address</th><th><div class=\"input-group\"><input class=\"form-control\" type=\"checkbox\"><span class=\"input-group-addon\" id=\"addon-type\"><div class=\"btn-group btn-toggle\"><button class=\"btn btn-xs btn-default\">ON</button><button class=\"btn btn-xs btn-primary active\">OFF</button></div></span></div></th></tr>\n<tr><th>h</th><th>help</th><th>Shows the help options</th><th><div class=\"input-group\"><input class=\"form-control\" type=\"text\"><span class=\"input-group-addon\" id=\"addon-type\">String</span></div></th></tr>\n<tr><th>h</th><th>help</th><th>Shows the help options</th><th><div class=\"input-group\"><input class=\"form-control\" type=\"text\"><span class=\"input-group-addon\" id=\"addon-type\">String</span></div></th></tr>\n<tr><th>h</th><th>help</th><th>Shows the help options</th><th><div class=\"input-group\"><input class=\"form-control\" type=\"text\"><span class=\"input-group-addon\" id=\"addon-type\">String</span></div></th></tr>\n<tr><th>h</th><th>help</th><th>Shows the help options</th><th><div class=\"input-group\"><input class=\"form-control\" type=\"text\"><span class=\"input-group-addon\" id=\"addon-type\">String</span></div></th></tr>\n\n</tbody>\n</table>\n</div>\n",
        providers: []
    }),
    __metadata("design:paramtypes", [])
], mainPage);
exports.mainPage = mainPage;
//# sourceMappingURL=app.main.component.js.map