cp ./configs/config_template_all_equipped.xml ./configs/config.xml
for mode in 'fixed' 'basic' 'communication' 'proportional'; do
    for agents in $(seq 1000 500 5000); do
        for i in $(seq 0 19); do
            echo "RUNNING ALL-EQUIPPED ${mode} ${agents} ${i}"
            if [ $mode == "fixed" ]; then
                mode_output="ftc"
            else
                mode_output=$mode
            fi
            if [ $mode == "communication" ] || [ $mode == "proportional" ]; then
                propagated="_prop"
            else
                propagated=""
            fi
            sed -i -e "s/TO_SET_AGENTS/${agents}/g" ./configs/config.xml
            sed -i -e "s/TO_SET_NUMBER/${i}/g" ./configs/config.xml
            sed -i -e "s/TO_SET_MODE/${mode}/g" ./configs/config.xml
            sed -i -e "s/TO_SET_OUTMODE/${mode_output}/g" ./configs/config.xml
            sed -i -e "s/_PROPAGATION/${propagated}/g" ./configs/config.xml
            java -Xmx6g -cp ./target/smartcity-0.0.1-SNAPSHOT.jar org.matsim.contrib.smartcity.RunSmartcity ./configs/config.xml
            cp ./configs/config_template_all_equipped.xml ./configs/config.xml
        done
    done
done
