import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { parse, stringify } from 'jsr:@libs/xml';
// const parser = new DOMParser();

const ext = process.argv[2] === undefined ? "lanes" : process.argv[2];
const format = ext == "single" ? singleLane : directLane;
const groupsFile = `SignalGroups_${ext}.xml`;
if(!existsSync(groupsFile)) {
    writeFileSync(groupsFile, '');
}
let xmlString = readFileSync(groupsFile, { encoding: 'utf8' });
if (xmlString.length === 0) { xmlString = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<signalGroups xsi:schemaLocation="http://www.matsim.org/files/dtd http://www.matsim.org/files/dtd/signalGroups_v2.0.xsd" xmlns="http://www.matsim.org/files/dtd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
</signalGroups>`;
}
// const xml = parser.parseFromString(xmlString);
const xml = parse(xmlString);
xml.signalGroups.signalSystem = [];
const systems = JSON.parse(readFileSync('signalGroups.json', { encoding: 'utf8' }));
for (let system of systems) {
    const ss = format(system);

    xml.signalGroups.signalSystem.push(ss);
}

// console.log(stringify(xml))
writeFileSync(groupsFile, stringify(xml));



function directLane(system) {
    return { 
        '@refId': system.id,
        signalGroup: [
            {
                "@id": "sr1",
                signal: [
                    { "@refId": `${system.n}.s` },
                    { "@refId": `${system.n}.r` },
                    { "@refId": `${system.s}.s` },
                    { "@refId": `${system.s}.r` },
                ]
            },
            {
                "@id": "sr2",
                signal: [
                    { "@refId": `${system.w}.s` },
                    { "@refId": `${system.w}.r` },
                    { "@refId": `${system.e}.s` },
                    { "@refId": `${system.e}.r` },
                ]
            },
            {
                "@id": "l1",
                signal: [
                    { "@refId": `${system.n}.l` },
                    { "@refId": `${system.s}.l` },
                ]
            },
            {
                "@id": "l2",
                signal: [
                    { "@refId": `${system.w}.l` },
                    { "@refId": `${system.e}.l` },
                ]
            }
        ]
    }
}

function singleLane(system) {
    return { 
        '@refId': system.id,
        signalGroup: [
            {
                "@id": system.n,
                signal: { "@refId": `${system.n}.ol` },
            },
            {
                "@id": system.s,
                signal: { "@refId": `${system.s}.ol` },
            },
            {
                "@id": system.w,
                signal: { "@refId": `${system.w}.ol` },
            },
            {
                "@id": system.e,
                signal: { "@refId": `${system.e}.ol` },
            }
        ]
    }
}
