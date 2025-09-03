import { expect } from '@playwright/test';

declare global {
    namespace PlaywrightTest {
        interface Matchers<R> {
            toBeRecentInSeconds(threshold?: number): R;
        }
    }
}

export {}; 
