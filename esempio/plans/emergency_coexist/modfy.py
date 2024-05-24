import random
import xml.etree.ElementTree as ET


for i in range(0, 2001, 200):
    for p in range(0,20):
        tree = ET.parse(f"{i}/plans{p}.xml")
        root = tree.getroot()
        for agent in root.iter('person'):
            for att in agent.find("attributes").findall("attribute"):
                if att.attrib['name'] == "budget":
                    if att.text != f'{i}' and random.randrange(100) >= 50:
                         agent.find("attributes").find("attribute[@name='drivelogic']").text = "org.matsim.contrib.smartcity.agent.StaticDriverLogic"

        tree.write(f"{i}/plans{p}.xml")