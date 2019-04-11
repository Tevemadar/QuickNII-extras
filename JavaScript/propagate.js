var fs=require("fs");
var propagation=require("./propagation.js");
var series=JSON.parse(fs.readFileSync(process.argv[2],"utf8"));
propagation.propagate(series.slices);
fs.writeFileSync(process.argv[3],JSON.stringify(series),"utf8");
