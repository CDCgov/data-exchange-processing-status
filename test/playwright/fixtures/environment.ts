
export type Environment = {
    environment: string;
    name: string;
    url: string;
}

export class Environments {
    envs: Array<Environment>;
    environments: Array<Environment> = JSON.parse(JSON.stringify(require('fixtures/environments.json')));
    constructor(private environment?: string) {
        this.envs = this.environments.filter(
            (env: any) => { return env.environment === this.environment })
    }
};
