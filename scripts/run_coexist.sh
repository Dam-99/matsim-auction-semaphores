cp ./configs/config_template_coexist.xml ./configs/config.xml
for traffic in 'high' 'medium' 'low'
do
    if [ $traffic == 'low' ]; then
        traffic_output="1000"
    elif [ $traffic == 'medium' ]; then
        traffic_output="2500"
    else
        traffic_output="4000"
    fi
    for mode in 'fixed' 'basic' 'communication' 'proportional'
    do
        for agents in `seq 20 20 80`
        do
            # for i in `seq 0 19`
            for i in `seq 0 4`
            do
                echo "RUNNING COEXIST ${traffic} ${mode} ${agents} ${i}"
                if [ $mode == "fixed" ]; then
                    mode_output="ftc"
                else
                    mode_output=$mode
                fi
                sed -i -e "s/TO_SET_TRAFFIC/${traffic}/g" ./configs/config.xml
                sed -i -e "s/TO_SET_OUTTRAFFIC/${traffic_output}/g" ./configs/config.xml
                sed -i -e "s/TO_SET_AGENTS/${agents}/g" ./configs/config.xml
                sed -i -e "s/TO_SET_NUMBER/${i}/g" ./configs/config.xml
                sed -i -e "s/TO_SET_MODE/${mode}/g" ./configs/config.xml
                sed -i -e "s/TO_SET_OUTMODE/${mode_output}/g" ./configs/config.xml
                java -Xmx6g -cp ./target/smartcity-0.0.1-SNAPSHOT.jar org.matsim.contrib.smartcity.RunSmartcity ./configs/config.xml
                cp ./configs/config_template_coexist.xml ./configs/config.xml
            done
        done
    done
done
