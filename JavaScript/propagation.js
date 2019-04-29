(function(exports){

    function propagate(arr){
        arr.sort(function(x,y){return x.nr-y.nr;});
        var linregs=[];
        for(var j=0;j<11;j++)
            linregs.push(new LinReg());
        var count=0;
        arr.forEach(function(item){
            if(item.anchoring){
                var w=item.anchoring;
                for(var j=0;j<3;j++)
                    w[j]+=(w[j+3]+w[j+6])/2;
                w.push(normalize(w,3)/item.width,normalize(w,6)/item.height);
                for(var j=0;j<w.length;j++)
                    linregs[j].add(item.nr,w[j]);
                count++;
            }
        });
        if(count>=2){
            var len=arr.length;
            if(!arr[0].anchoring){
                var nr=arr[0].nr;
                var w=[];
                for(var j=0;j<linregs.length;j++)
                    w.push(linregs[j].get(nr));
                orthonormalize(w);
                arr[0].anchoring=w;
                count++;
            }
            if(!arr[len-1].anchoring){
                var nr=arr[len-1].nr;
                var w=[];
                for(var j=0;j<linregs.length;j++)
                    w.push(linregs[j].get(nr));
                orthonormalize(w);
                arr[len-1].anchoring=w;
                count++;
            }
            var start=1;
            while(count<len){
                while(arr[start].anchoring)start++;
                var next=start+1;
                while(!arr[next].anchoring)next++;
                var pnr=arr[start-1].nr;
                var nnr=arr[next].nr;
                var panch=arr[start-1].anchoring;
                var nanch=arr[next].anchoring;
                var linints=[];
                for(var j=0;j<panch.length;j++)
                    linints.push(new LinInt(pnr,panch[j],nnr,nanch[j]));
                for(var i=start;i<next;i++){
                    var nr=arr[i].nr;
                    var anchoring=[];
                    for(var j=0;j<linints.length;j++)
                        anchoring.push(linints[j].get(nr));
                    arr[i].anchoring=anchoring;
                    count++;
                }
                start=next+1;
            }
        }
        arr.forEach(function(item){
            var w=item.anchoring;
            orthonormalize(w);
            var v=w.pop();
            var u=w.pop();
            for(var i=0;i<3;i++){
                w[i+3]*=u*item.width;
                w[i+6]*=v*item.height;
                w[i]-=(w[i+3]+w[i+6])/2;
            }
        });
    }

    function LinInt(x1,y1,x2,y2){
        this.get=function(x){return y1+(y2-y1)*(x-x1)/(x2-x1);};
    }

    function LinReg(){
        var n=0;
        var Sx=0;
        var Sy=0;
        var Sxx=0;
        var Sxy=0;
        var a,b;
        this.add=function(x,y){
            n++;
            Sx+=x;
            Sy+=y;
            Sxx+=x*x;
            Sxy+=x*y;
            if(n>=2){
                b=(n*Sxy-Sx*Sy)/(n*Sxx-Sx*Sx);
                a=Sy/n-b*Sx/n;
            }
        };
        this.get=function(x){return a+b*x;};
    }

    function normalize(arr,idx){
        var len=0;
        for(var i=0;i<3;i++)
            len+=arr[idx+i]*arr[idx+i];
        len=Math.sqrt(len);
        for(var i=0;i<3;i++)
            arr[idx+i]/=len;
        return len;
    }

    function orthonormalize(arr){
        normalize(arr,3);
        var dot=0;
        for(var i=0;i<3;i++)
            dot+=arr[i+3]*arr[i+6];
        for(var i=0;i<3;i++)
            arr[i+6]-=arr[i+3]*dot;
        normalize(arr,6);
    }

    exports.propagate=propagate;

})(typeof exports==="undefined"?this["propagation"]={}:exports);
