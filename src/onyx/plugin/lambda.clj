(ns onyx.plugin.lambda
  (:require [onyx.plugin.onyx-plugin :as onyx-plugin]
            [onyx.plugin.onyx-input :as onyx-input]
            [onyx.lambda.extensions :as lambda.extensions]))

(defn parse-input [event]
  (let [lambda-request (:lambda/lambda-request event)
        event-source (get-in event [:task-map :lambda/event-source])]
    (lambda.extensions/lambda-request-segments event-source lambda-request)))

(defrecord LambdaInputReader [event sequential rst segment offset]
  onyx-plugin/OnyxPlugin
  (start [this]
    (let [sequential (parse-input event)] 
      (assoc this :rst sequential :sequential sequential :offset -1)))

  (stop [this event] 
    (assoc this :rst nil :sequential nil))

  onyx-input/OnyxInput

  (checkpoint [this]
    offset)

  (recover [this checkpoint]
    (if (nil? checkpoint) 
      (assoc this 
             :rst sequential 
             :offset -1)
      (assoc this 
             :rst (drop (inc checkpoint) sequential)
             :offset checkpoint)))

  (offset-id [this]
    offset)

  (segment [this]
    segment)

  (next-state [this event]
    (let [segment (first rst)
          remaining (rest rst)]
      (assoc this
             :segment segment
             :rst remaining
             :offset (if segment (inc offset) offset))))

  (segment-complete! [this segment])

  (completed? [this]
    (empty? rst)))

(defn input [event]
  (map->LambdaInputReader {:event event}))

(defn inject-lambda-request [event {:keys [lambda/lambda-request]}]
  {:lambda/lambda-request lambda-request})

(def lifecycle-calls
  {:lifecycle/before-task-start inject-lambda-request})
