function test() {
    let value = 'test';
    let secret = 'test';
    let algorithm = 'MD5';

    return $.hmac(value, secret, algorithm);
}

let _structrMainResult = test();