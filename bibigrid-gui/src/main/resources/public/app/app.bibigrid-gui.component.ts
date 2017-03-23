import {Component} from "@angular/core";
import {welcomePage} from './welcome_page/app.welcome-page.component';
import {mainPage} from "./core/app.main.component"


@Component({
    selector: 'bibigui',
    template: `
<welcome (notify)='setMode($event)' *ngIf="welcomePage" ></welcome>
<mainpage [userMode]='mode' *ngIf="!welcomePage"></mainpage>
`,
    providers: []
})

export class BiBiGridGui {

    welcomePage: boolean = true;
    mode: string;

    setMode(mode: string): void {
        this.welcomePage = !this.welcomePage;
        this.mode = mode;
    }

    constructor() {
    }
}

