import {Injectable}     from '@angular/core';
import {Http, Response} from '@angular/http';
import {Headers, RequestOptions} from '@angular/http';
import {Observable}     from 'rxjs/Observable';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/catch';
import 'rxjs/add/observable/throw';

import {Flag} from '../shared/flag';
import {presetFlag} from '../shared/presetFlag';
import {configLink} from '../shared/configLink';
import {JsonWrapper} from './json.wrapper'


@Injectable()
export class ServerCommunication {

    private getUrl: string = "http://localhost:8080/resource";
    private sendUrl: string = "http://localhost:8080//process";
    private ansUrl: string = "http://localhost:8080/answer";

    constructor(private http: Http) {
    }

    getConfig(confPath: string): Observable<presetFlag[]> {
        return this.http.get(confPath)
            .map(this.extractData)
            .catch(this.handleError);
    }

    getConfLinks(confPath: string): Observable<configLink[]> {
        return this.http.get(confPath)
            .map(this.extractData)
            .catch(this.handleError);
    }

    getFlags(): Observable<Flag[]> {
        return this.http.get(this.getUrl)
                .map(this.extractData)
                .catch(this.handleError);
    }

    getAnswer(): Observable<string[]> {
        return this.http.get(this.ansUrl)
            .map(this.extractData)
            .catch(this.handleError);
    }

    sendFlags (flags: Flag[]): Observable<Flag[]> {
        let headers = new Headers({ 'Content-Type': 'application/json' });
        let options = new RequestOptions({ headers: headers });

        return this.http.post(this.sendUrl, JSON.stringify(new JsonWrapper(flags)) , options)
            .map(this.extractData)
            .catch(this.handleError);
    }

    private extractData(res: Response) {
        let body = res.json();
        console.log(body);
        return body.data || {};
    }

    private handleError(error: Response | any) {
        let errMsg: string;
        if (error instanceof Response) {
            const body = error.json() || '';
            const err = body.error || JSON.stringify(body);
            errMsg = `${error.status} - ${error.statusText || ''} ${err}`;
        } else {
            errMsg = error.message ? error.message : error.toString();
        }
        console.error(errMsg);
        return Observable.throw(errMsg);
    }
}