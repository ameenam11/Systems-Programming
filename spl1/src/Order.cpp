#include "../include/Order.h"
#include <string>
#include <vector>
#include <stdexcept>
using std::string;
using std::vector;



    

        Order::Order(int id, int customerId, int distance) 
        : id(id), customerId(customerId), distance(distance), status(OrderStatus::PENDING), collectorId(NO_VOLUNTEER),driverId(NO_VOLUNTEER)
        {}
        int Order::getId() const
        {
            return id;
        }
        int Order::getCustomerId() const
        {
            return customerId;
        }
        void Order::setStatus(OrderStatus status)
        {
            this->status = status;
        }
        void Order::setCollectorId(int collectorId)
        {
            this->collectorId = collectorId;
        }
        void Order::setDriverId(int driverId)
        {
            this->driverId = driverId;
        }
        int Order::getCollectorId() const
        {
            return this->collectorId;
        }
        int Order::getDriverId() const
        {
            return this->driverId;
        }

        int Order::getDistance() const
        {
            return distance;
        }


        OrderStatus Order::getStatus() const
        {
            return this->status;
        }
        string Order::statusString(OrderStatus status) const
        {
            if(status == OrderStatus::PENDING)
                return "Pending";
            if(status == OrderStatus::COLLECTING)
                return "Collecting";
            if(status == OrderStatus::DELIVERING)
                return "Delivering";
            if(status == OrderStatus::COMPLETED)
                return "Completed";
            return "";
        }
        const string Order::toString() const
        {
            string jump = "\n";
            string s1 = "OrderId: " +  std::to_string(id);
            string s2 = statusString(status);
            string s3 = "OrderStatus: " + s2;
            string s4 = "CustomerID: " +  std::to_string(customerId);
            string s5,s6 = "";
            if(collectorId == -1)
                s5 = "Collector: None";    
            else
                s5 = "Collector: " +  std::to_string(collectorId);
            if(driverId == -1)
                s6 = "Driver: None";    
            else
                s6 = "Driver: " +  std::to_string(driverId);
            string s = s1 + jump + s3 + jump + s4 + jump + s5 + jump + s6;

            return s;        
        }
           


        









