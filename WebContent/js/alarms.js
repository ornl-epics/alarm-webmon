let AlarmIcons =
{
    "OK": undefined,
    "MINOR_ACK": "icons/minor_ack.png",
    "MAJOR_ACK": "icons/major_ack.png",
    "INVALID_ACK": "icons/undefined_ack.png",
    "UNDEFINED_ACK": "icons/undefined_ack.png",
    "MINOR": "icons/minor.png",
    "MAJOR": "icons/major.png",
    "INVALID": "icons/undefined.png",
    "UNDEFINED": "icons/undefined.png"
};

class Alarms
{
    constructor()
    {
    }

    showError(text)
    {
        jQuery("#status").text(text).addClass("UNDEFINED");

    }
 
    showStatus(text)
    {
        jQuery("#status").text(text).removeClass("UNDEFINED");
    }
 
    now()
    {
        let d = new Date();
        var datestring = d.getFullYear()+ "-" + ("0"+(d.getMonth()+1)).slice(-2)  + "-" +  ("0" + d.getDate()).slice(-2)
                         + " "
                         + ("0" + d.getHours()).slice(-2) + ":" + ("0" + d.getMinutes()).slice(-2);
        return datestring;
    }
    
    update()
    {
        console.log("Updating....");
        
        jQuery.get("alarms", data =>
                   {
                       // console.log(data);
                       this.showAlarms(data.active, "active");
                       this.showAlarms(data.acknowledged, "acknowledged");
                       this.showStatus("Last update: " + this.now());
                   })
              .fail((xhr, status, error) =>
                    {
                       console.log("Error:");
                       console.log(xhr);
                       console.log(status);
                       console.log(error);
                       this.showError("Failed to fetch update: " + this.now());
                    });    
    }
    
    showAlarms(data, which)
    {
        // Remove old data
        let body = jQuery("#" + which + " tbody");
        body.html("");
        
        // Show new data
        if (data === undefined  ||   data.length <= 0)
        {
            let info = jQuery("<td>").attr("colspan", 9).attr("align", "center").text("- There are no " + which + " alarms -");
            body.html( jQuery("<tr>").append(info) );  
        }
        else
            for (const pv of data)
            {
                let row = jQuery("<tr>");
                let icon = AlarmIcons[pv.severity];
                
                if (icon === undefined)
                    row.append(jQuery("<td>"));
                else
                    row.append(jQuery("<td>").append(jQuery("<img>").attr("src", icon)));
                row.append(jQuery("<td>").text(pv.name));
                row.append(jQuery("<td>").text(pv.description));
                row.append(jQuery("<td>").addClass(pv.severity).text(pv.severity));
                row.append(jQuery("<td>").text(pv.message));
                row.append(jQuery("<td>").text(pv.time));
                row.append(jQuery("<td>").text(pv.value));
    
                
                
                row.append(jQuery("<td>").addClass(pv.current_severity).text(pv.current_severity));
                row.append(jQuery("<td>").text(pv.current_message));
                body.append(row);
            }

        // Check if rows needs to be sorted
        let headers = jQuery("#" + which + " thead").find("th");
        for (let i=0; i<headers.length; ++i)
        {
            let header = jQuery(headers[i]);
            if (header.hasClass("sorttable_sorted"))
            {
                // console.log("Sorted by column " + i);
                // Unclear how to effectively re-sort.
                // When a column is already marked as sorted,
                // sorttable.js will simply reverse the rows.
                // So mark as not sorted, and then fake a click that'll sort it.
                header.removeClass("sorttable_sorted");
                header.click();
            }
            else if (header.hasClass("sorttable_sorted_reverse"))
            {
                // console.log("Reverse-Sorted by column " + i);
                header.removeClass("sorttable_sorted_reverse");
                // Similar to forward-sorted case, but click twice:
                // First to sort, then to reverse
                header.click();
                header.click();                
            }
        }
    }
}

alarms = new Alarms();

// jQuery("th").click(e => console.log(jQuery(e.target)));
