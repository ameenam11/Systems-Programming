#include <string>
#include <vector>
#include <iostream>
#include "../include/Volunteer.h"
#include"../include/Customer.h"
#include"../include/WareHouse.h"
#include"../include/Action.h"
using std::string;
using std::vector;
using namespace std;


// enum class ActionStatus{
//     COMPLETED, ERROR
// };

// enum class CustomerType{
//     Soldier, Civilian
// };



        BaseAction::BaseAction(): errorMsg("no actions have been made yet"),
            status(ActionStatus::ERROR)
        {
        }
        ActionStatus BaseAction::getStatus() const
        {
            return status;
        }

    
        void BaseAction::complete()
        {
            status = ActionStatus::COMPLETED;
        }
        void BaseAction::error(string errorMsg)
        {
            status = ActionStatus::ERROR;
            this->errorMsg = errorMsg;
        }
        string BaseAction::getErrorMsg() const
        {
            return errorMsg;
        }

        string BaseAction:: getStringStatus() const{
            if(this->status == ActionStatus::COMPLETED){
                return "COMPLETED";
            }else{
                return "ERROR: (" + this->errorMsg + ")";
            }
        }


        SimulateStep::SimulateStep(int numOfSteps)
        : BaseAction(), numOfSteps(numOfSteps)
        {}
        void SimulateStep::act(WareHouse &wareHouse) 
        {
            for(int i=0; i<numOfSteps; i++)
            {
                wareHouse.step();
            }
            this->complete();
            wareHouse.addAction(this);
        }
        std::string SimulateStep::toString() const 
        {
            return "simulateStep " + std::to_string(numOfSteps) + " " + getStringStatus();
        }
        SimulateStep* SimulateStep::clone() const 
        {
            return new SimulateStep(*this);
        }

  



        AddOrder::AddOrder(int id)
        : BaseAction(), customerId(id)
        {}
        
        void AddOrder::act(WareHouse &wareHouse)
        {
            
            if(customerId < wareHouse.getCustomerCounter())
            {
                Customer* customer = &wareHouse.getCustomer(customerId);
                int check = customer->addOrder(wareHouse.getOrderCounter());
                if(check != -1)
                {
                    wareHouse.addOrder(new Order(wareHouse.getOrderCounter(), customerId, customer->getCustomerDistance()));
                    this->complete();
                }else{
                    cout << "Error: Cannot place this order" << endl;
                    this->error(customer->getName() + " reached his maxOrders limit");
                }
            }else{
                cout << "Error: Cannot place this order" << endl;
                this->error("Cannot place this order");
            }
            wareHouse.addAction(this);
           
        }
        string AddOrder::toString() const
        {
            return "order " + std::to_string(customerId)+ " " + this->getStringStatus();
        }
        AddOrder* AddOrder::clone() const 
        {
            return new AddOrder(*this);
        }




        AddCustomer::AddCustomer(const string &customerName, const string &customerType, int distance, int maxOrders):
        customerName(customerName), customerType(stringToCusType(customerType)), distance(distance), maxOrders(maxOrders){}
        
        void AddCustomer:: act(WareHouse &wareHouse){
            Customer* newCustomer;
            if(customerType == CustomerType::Soldier){
                newCustomer = new SoldierCustomer(wareHouse.getCustomerCounter(), customerName, distance, maxOrders);
            }else{
                newCustomer = new CivilianCustomer(wareHouse.getCustomerCounter(), customerName, distance, maxOrders);
            }

            wareHouse.addCustomer(newCustomer);
            this->complete();
            wareHouse.addAction(this);
        }
        AddCustomer* AddCustomer::clone() const 
        {
            return new AddCustomer(*this);
        }
        string AddCustomer:: toString() const{
            string s1("customer " + customerName+ " ");
            string s2 = "";
            if(customerType == CustomerType::Soldier){
                s2 = "soldier ";
            }else{
                s2 = "civilian ";
            }
            string s3(std::to_string(distance)+" ");
            string s4(std::to_string(maxOrders));
            return s1+s2+s3+s4 + " " + getStringStatus();
        }
        CustomerType AddCustomer:: stringToCusType(const string &customerType){
            if(customerType == "Civilian"){
                return CustomerType::Civilian;
            }else{
                return CustomerType::Soldier;
            }
        }




        PrintOrderStatus::PrintOrderStatus(int id)
        : BaseAction(), orderId(id)
        {}
        void PrintOrderStatus::act(WareHouse &wareHouse) 
        {
            if(orderId < wareHouse.getOrderCounter())
            {
                Order order = wareHouse.getOrder(orderId);
                cout << order.toString() << endl;
                this->complete();
            }else {
                cout << "Order doesn't exist" << endl;
                this->error("Order doesn't exist");
            }
            wareHouse.addAction(this);

        }
        PrintOrderStatus* PrintOrderStatus::clone() const 
        {
            return new PrintOrderStatus(*this);
        }
        string PrintOrderStatus::toString() const
        {
            
            return "orderStatus " + std::to_string(orderId) + " " + getStringStatus();
        }


  PrintCustomerStatus::PrintCustomerStatus(int customerId): 
        BaseAction(), customerId(customerId) 
        {}
    void PrintCustomerStatus:: act(WareHouse &wareHouse)
    {
        if(customerId < wareHouse.getCustomerCounter())
        {
            Customer* customer = &wareHouse.getCustomer(customerId);
            string result("");
            result += "CustomerID: " + std::to_string(customerId);
            for(int orderId: customer->getOrdersIds())
            {
                result += "\nOrderID: " + std::to_string(orderId);
                Order* order = &wareHouse.getOrder(orderId);
                result += "\nOrderStatus: " + order->statusString(order->getStatus());
            }
            result += "\nnumOrdersLeft: " + std::to_string(customer->getMaxOrders() - customer->getNumOrders());
            cout << result << endl;
            this->complete();
        }else{
            cout << "Error: Customer doesn't exist" << endl;
            this->error("Customer doesn't exist");
        }
        
        wareHouse.addAction(this);


    }
        PrintCustomerStatus* PrintCustomerStatus::clone() const 
        {
            return new PrintCustomerStatus(*this);
        }
        string PrintCustomerStatus:: toString() const
        {
            return "customerStatus " + std::to_string(customerId) + " " + this->getStringStatus();
        }
   





 PrintVolunteerStatus::PrintVolunteerStatus(int id) : volunteerId(id)
 {}
        
        void PrintVolunteerStatus:: act(WareHouse &wareHouse) 
        {
            Volunteer* volunteer = &wareHouse.getVolunteer(this->volunteerId);

            if(volunteer->getId() >= 0)
            {
                cout<< volunteer->toString() <<endl;
                this->complete();
                
            }else{
                delete volunteer;
                cout<< "Error: Volunteer doesn't exist"<<endl;
                this->error("Volunteer doesn't exist"); 
            }
            
            wareHouse.addAction(this);
        }
        PrintVolunteerStatus* PrintVolunteerStatus::clone() const 
        {
            return new PrintVolunteerStatus(*this);
        }
        string PrintVolunteerStatus:: toString() const
        {
            return "volunteerStatus " + std::to_string(volunteerId) + " " + this->getStringStatus();
        }
  



        PrintActionsLog::PrintActionsLog(): BaseAction()
        {}

        void PrintActionsLog:: act(WareHouse &wareHouse)
        {
            for(BaseAction * action: wareHouse.getActions())
            {
                cout<< action->toString() << endl;
            }

            this->complete();
            wareHouse.addAction(this);
        }
        PrintActionsLog* PrintActionsLog::clone() const  
        {
            return new PrintActionsLog(*this);
        }
        string PrintActionsLog::toString() const
        {
            return "log " + this->getStringStatus();
        }

        Close::Close(): BaseAction()
        {}
        void Close:: act(WareHouse &wareHouse)
        {
            for(Order* order: wareHouse.getPendingOrders())
            {
                string s1("OrderID: " + std::to_string(order->getId()) + ", ");
                string s2("CustomerID: " + std::to_string(order->getCustomerId()) + ", ");
                string s3("Status: " + order->statusString(order->getStatus()));
                cout << s1 + s2 + s3 << endl;
            }

            for(Order* order: wareHouse.getInProcessOrders())
            {
                string s1("OrderID: " + std::to_string(order->getId()) + ", ");
                string s2("CustomerID: " + std::to_string(order->getCustomerId()) + ", ");
                string s3("Status: " + order->statusString(order->getStatus()));
                cout << s1 + s2 + s3 << endl;
            }

            for(Order* order: wareHouse.getCompletedOrders())
            {
                string s1("OrderID: " + std::to_string(order->getId()) + ", ");
                string s2("CustomerID: " + std::to_string(order->getCustomerId()) + ", ");
                string s3("Status: " + order->statusString(order->getStatus()));
                cout << s1 + s2 + s3 << endl;
            }

            this->complete();
            wareHouse.addAction(this);
            wareHouse.close();
        }
        Close* Close::clone() const 
        {
            return new Close(*this);
        }
        string Close:: toString() const 
        {
            return "close " + this->getStringStatus();
        }



        BackupWareHouse::BackupWareHouse()
        : BaseAction()
        {}
        void BackupWareHouse::act(WareHouse &wareHouse) 
        {
            
            if(wareHouse.getIsBackedUp()){
                delete backup;
                // backup = nullptr;
            }
            backup = new WareHouse(wareHouse);
            
            this->complete();
            wareHouse.addAction(this);
            wareHouse.backUpWareHouse();
        }
        BackupWareHouse* BackupWareHouse::clone() const 
        {
            return new BackupWareHouse(*this);
        }
        string BackupWareHouse::toString() const 
        {
            return "backup " + this->getStringStatus();
        }



        RestoreWareHouse::RestoreWareHouse()
        : BaseAction()
        {}
        void RestoreWareHouse::act(WareHouse &wareHouse) 
        {
            if(wareHouse.getIsBackedUp()){
                wareHouse = *backup;
                this->complete();
            }else{
                cout << "No backup available" << endl;
                this->error("No backup available");
            }
            
            wareHouse.addAction(this);
        }
        RestoreWareHouse* RestoreWareHouse::clone() const 
        {
            return new RestoreWareHouse(*this);
        }
        string RestoreWareHouse::toString() const 
        {
            return "restore " + this->getStringStatus();
        }