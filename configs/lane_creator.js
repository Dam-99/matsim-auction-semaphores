import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { parse, stringify } from 'jsr:@libs/xml';
// const parser = new DOMParser();

const lenLink = 300.0;
const lenLane = lenLink / 2.0; // for some reason they make it as half the lane in the examples...
const vphLink = 800.0; // divided by two in lanes??
const vph = vphLink / 2.0;
const ext = process.argv[2] === undefined ? "lanes" : process.argv[2];
const format = ext == "single" ? singleLane : directLane;
const lanesFile = `lanes_${ext}.xml`;
if(!existsSync(lanesFile)) {
    writeFileSync(lanesFile, '');
}
let xmlString = readFileSync(lanesFile, { encoding: 'utf8' });
if (xmlString.length === 0) { xmlString = `<?xml version="1.0" standalone="yes"?>
<laneDefinitions xsi:schemaLocation="http://www.matsim.org/files/dtd http://www.matsim.org/files/dtd/laneDefinitions_v2.0.xsd" xmlns="http://www.matsim.org/files/dtd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">`;
}
// const xml = parser.parseFromString(xmlString);
const xml = parse(xmlString);
xml.laneDefinitions.lanesToLinkAssignment = [];
const links = JSON.parse(readFileSync('lanes.json', { encoding: 'utf8' }));
for (let link of links) {
    const ltla = format(link);

    xml.laneDefinitions.lanesToLinkAssignment.push(ltla);
}

// console.log(stringify(xml))
writeFileSync(lanesFile, stringify(xml));




function directLane(link) {
    return { 
        '@linkIdRef': link.id,
        lane: [
            { // LANE TURNING LEFT
            '@id': link.id + ".l",
            leadsTo: { toLink: { '@refId': link.left } },
            representedLanes: { '@number': "1.0" },
            capacity: { '@vehiclesPerHour': vph },
            startsAt: { '@meterFromLinkEnd': lenLane },
            alignment: "1",
            attributes: {},
            },
            { // ORIGINAL LANE
            '@id': link.id + ".ol",
            leadsTo: { toLane: [ { '@refId': link.id + ".l" }, { '@refId': link.id + ".s" }, { '@refId': link.id + ".r" } ] },
            representedLanes: { '@number': "1.0" },
            capacity: { '@vehiclesPerHour': vphLink },
            startsAt: { '@meterFromLinkEnd': lenLink },
            alignment: "0",
            attributes: {},
            },
            { // LANE TURNING RIGHT
            '@id': link.id + ".r",
            leadsTo: { toLink: { '@refId': link.right } },
            representedLanes: { '@number': "1.0" },
            capacity: { '@vehiclesPerHour': vph },
            startsAt: { '@meterFromLinkEnd': lenLane },
            alignment: "-1",
            attributes: {},
            },
            { // LANE GOING STRAIGHT
            '@id': link.id + ".s",
            leadsTo: { toLink: { '@refId': link.straight } },
            representedLanes: { '@number': "1.0" },
            capacity: { '@vehiclesPerHour': vph },
            startsAt: { '@meterFromLinkEnd': lenLane },
            alignment: "0",
            attributes: {},
            },
        ]
    }
}

function singleLane(link) {
    return { 
        '@linkIdRef': link.id,
        lane: {
            '@id': link.id + ".ol", // .ol probs needed to not break the code
            leadsTo: {
                toLink: [
                    { '@refId': link.left },
                    { '@refId': link.straight },
                    { '@refId': link.right } 
                ],
            },
            representedLanes: { '@number': "1.0" },
            capacity: { '@vehiclesPerHour': vphLink },
            startsAt: { '@meterFromLinkEnd': lenLink },
            alignment: "0",
            attributes: {},
        }
    }
}

