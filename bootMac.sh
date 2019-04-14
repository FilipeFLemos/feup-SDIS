osascript -e 'tell app "Terminal"
    do script "cd ~/FEUP/SDIS/SDIS/
                sh reset.sh
                sh compile.sh
                sh rmi.sh"
end tell'

sleep 1

osascript -e 'tell app "Terminal"
    do script "cd ~/FEUP/SDIS/SDIS/
                sh run.sh 1.0 1 127.0.0.1"
end tell'

osascript -e 'tell app "Terminal"
    do script "cd ~/FEUP/SDIS/SDIS/
                sh run.sh 1.0 2 127.0.0.1"
end tell'

osascript -e 'tell app "Terminal"
    do script "cd ~/FEUP/SDIS/SDIS/
                sh run.sh 1.0 3 127.0.0.1"
end tell'

