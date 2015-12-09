package user.totekp.pure

// #CodeCoverage 100%
object StatsMath  {

  def log(in: Double, base: Int): Double = {
    math.log(in) / math.log(base)
  }

  def ln(in: Double): Double = {
    math.log(in)
  }

  def stdevP(values: Seq[Double]): Double = {
    val avg: Double = average(values)
    assert(values.nonEmpty, "values.size > 0")
    math.sqrt(values.foldLeft(0d)((acc, v) => acc + math.pow(v - avg, 2)) / values.size)
  }

  def stdevS(values: Seq[Double]): Double = {
    val avg: Double = average(values)
    assert(values.nonEmpty, "values.size > 0")
    math.sqrt(values.foldLeft(0d)((acc, v) => acc + math.pow(v - avg, 2)) / (values.size - 1))
  }

  def average(values: Seq[Double]): Double = {
    if (values.isEmpty) {
      throw new Exception("Size must be > 0")
    }
    values.sum / values.length
  }

  def standardize(x: Double, mean: Double, sd: Double): Double = {
    (x - mean) / sd
  }

  /**
    * https://support.office.com/en-us/article/RSQ-function-d7161715-250d-4a01-b80d-a8364f2be08f
    * @param xValues
    * @param yValues
    * @param isNotOutlier
    */
  def rsq(xValues: Seq[Double], yValues: Seq[Double], isNotOutlier: ((Double, Double)) => Boolean): Double = {
    assert(xValues.size == yValues.size, "X and Y values must have same size")
    assert(xValues.nonEmpty, "values cannot be empty")
    val averageX = average(xValues)
    val averageY = average(yValues)
    val pairs= xValues.zip(yValues)
    val filteredPairs = pairs.filter(isNotOutlier)
    val numerator = filteredPairs.foldLeft(0d) { case (acc, (xx, yy)) =>
      acc + ((xx - averageX) * (yy - averageY))
    }
    val (sumXSqr, sumYSqr) = filteredPairs.foldLeft((0d, 0d)) { case ((accX, accY), (xx, yy)) =>
      (accX + math.pow(xx - averageX, 2), accY + math.pow(yy - averageY, 2))
    }
    val r = numerator / math.sqrt(sumXSqr * sumYSqr)
    val rsq = math.pow(r, 2)
    rsq
  }

  def curveSimilarity(range0: Seq[Double], range1: Seq[Double]): Vector[Double] = {
    assert(range0.nonEmpty, "range0 should be nonempty")
    assert(range0.size == range1.size, "range0 and range1 should have same size")
    val N = range0.size
    val residualSqs: Seq[Double] = range0.zip(range1).map { case (aa, bb) => math.pow(aa - bb, 2) }
    val residualSqSum = range0.zip(range1).foldLeft(0d) { case (acc, (aa, bb)) => acc + math.pow(aa - bb, 2) }
    val averageResidualSq = residualSqSum / range0.size
    val stdevSResidualSq = stdevS(residualSqs)

    val skewnessResidualSq: Double = {
      residualSqs.foldLeft(0d)((acc, aa) => acc + math.pow(aa - averageResidualSq, 3)) / ((N - 1).toDouble * math.pow(stdevSResidualSq, 3))
    }
    val kurtosisResidualSq: Double = {
      residualSqs.foldLeft(0d)((acc, aa) => acc + math.pow(aa - averageResidualSq, 4)) / ((N - 1).toDouble * math.pow(stdevSResidualSq, 4))
    }
    Vector(averageResidualSq, stdevSResidualSq, skewnessResidualSq, kurtosisResidualSq)
  }

  def lnDeltas(values: Seq[Double]): Iterator[Double] = {
    values.sliding(2).map {
      case Seq(aa, bb) =>
        ln(bb / aa)
    }
  }

  def sma(values: Seq[Double], period: Int): Vector[Double] = {
    require(period > 0 && period <= values.size, "Period is out of bound")
    val result = values.take(period - 1).toVector ++ values.sliding(period).toVector.map {
      window =>
        window.foldLeft(0d)(_ + _) / period
    }
    assert(result.size == values.size, "sizes are not equal")
    result
  }

  def emaWithAlpha(values: Seq[Double], period: Int, alpha: Double): Vector[Double] = {
    require(period > 0 && period <= values.size, "Period is out of bound")
    values.foldLeft(Vector.empty[Double]) {
      case (acc, value) if acc.isEmpty =>
        acc :+ value.toDouble
      case (acc, value) =>
        val ema_prev = acc.last
        acc :+ ((value * alpha) + (ema_prev * (1d - alpha)))
    }
  }

  def ema(values: Seq[Double], period: Int): Vector[Double] = {
    require(period > 0 && period <= values.size, "Period is out of bound")
    val k: Double = 2d / (period + 1) // smoothing factor
    emaWithAlpha(values, period, k)
  }

  def macdLine(values: Seq[Double], shortPeriod: Int = 12, longPeriod: Int = 26): Vector[Double] = {
    val emaShort = ema(values, shortPeriod)
    val emaLong = ema(values, longPeriod)
    val macdLine = (emaShort, emaLong).zipped.map {
      case (e2, e1) =>
        e2 - e1
    }
    macdLine
  }

  def macdSignalLine(values: Seq[Double], shortPeriod: Int = 12, longPeriod: Int = 26, signalPeriod: Int = 9): Vector[Double] = {
    ema(macdLine(values, shortPeriod, longPeriod), signalPeriod)
  }

  def macdHistogram(values: Seq[Double], shortPeriod: Int = 12, longPeriod: Int = 26, signalPeriod: Int = 9): Vector[Double] = {
    val _macdLine = macdLine(values, shortPeriod, longPeriod)
    val _signalLine = macdSignalLine(values, shortPeriod, longPeriod, signalPeriod)
    val macdHistogram = (_macdLine, _signalLine).zipped.map {
      case (e2, e1) =>
        e2 - e1
    }
    macdHistogram
  }

  def entropyFromProbabilities(ps: Seq[Double]): Double = {
    val validPs = validProbabilities(ps)
    if (validPs.isEmpty) {
      throw new Exception("validPs.size must be > 0")
    }
    validPs.foldLeft(0d)((acc, a) => acc + (- a * log(a, 2)))  }

  def entropyFromFrequencies(fs: Seq[Double]): Double = {
    val sum = fs.sum
    val ps = fs.map(_ / sum)
    entropyFromProbabilities(ps)
  }

  def validProbabilities(ps: Seq[Double]): Seq[Double] = {
    if (ps.sum.round != 1d)
      throw new Exception("Probabilities do not sum to 1")

    if (ps.min < 0)
      throw new Exception("Negative probability")
    ps.filter(_ > 0)
  }

  // uniform funs
  def getBinaryDigitsPerSymbolUniform(sampleSize: Double): Double = {
    StatsMath.log(sampleSize, 2).ceil
  }

  def getRateFromUniform(channelCapacity: Double, sampleSize: Double, numChained: Double): Double = {
    // (binary digits/symbol)
    val LL = getBinaryDigitsPerSymbolUniform(math.pow(sampleSize, numChained)) / numChained
    // actual communication rate (symbol/s)
    val RR = channelCapacity / LL
    RR
  }

  def getEfficiencyFromUniform(sampleSize: Double, numChained: Double): Double = {
    // entropy (bits/symbol)
    val HH = StatsMath.log(sampleSize, 2)
    // (binary digits/symbol)
    val LL = getBinaryDigitsPerSymbolUniform(math.pow(sampleSize, numChained)) / numChained
    // Coding efficiency (bits/binary digit)
    val EE = HH / LL
    EE
  }
}

// #CodeCoverage 100%
case class ConfusionMatrix2(testP: Double, actualP: Double, trueP: Double) {
  private def isProbability(in: Double) = in >= 0d && in <= 1d
  assert(isProbability(testP), "testP should be probability")
  assert(isProbability(actualP), "actualP should be probability")
  assert(isProbability(trueP), "trueP should be probability")

  import StatsMath._
  val testN: Double = 1d - testP
  val actualN: Double = 1d - actualP

  /**
    * actual Positive
    */
  val aa: Double = actualP

  /**
    * actual Negative
    */
  val bb: Double = actualN

  /**
    * test Positive
    */
  val cc: Double = testP

  /**
    * test Negative
    */
  val dd: Double = testN

  /**
    * true Positive
    */
  val ee: Double = trueP

  val TP: Double = ee

  /**
    * false Negative
    */
  val ff: Double = aa - ee

  val falseN: Double = ff

  val FN: Double = ff

  /**
    * false Positive
    */
  val gg: Double = cc - ee

  val falseP: Double = gg

  val FP: Double = gg

  /**
    * true Negative
    */
  val hh: Double = bb - gg

  val trueN: Double = hh

  val TN: Double = hh

  // conditional properties
  val true_P_rate: Double = ee / aa
  val false_N_rate: Double = ff / aa
  val false_P_rate: Double = gg / bb
  val true_N_rate: Double = hh / bb

  /**
    * Positive predictive value
    * p("+" | Test POS)
    */
  val ppv: Double = ee / cc

  /**
    * Negative predictive value
    * p("-" | Test NEG)
    */
  val npv: Double = hh / dd

  /**
    * Definition of Independence P(X,Y) = P(X)p(Y)
    */
  val isIndependent: Boolean = {
    // test TP
    val ac = this.aa * this.cc
    ac == ee
  }

  // information metrics

  /**
    * H(X) 	= a*log(1/a)	+ b*log(1/b)
    * 0.7219	0.4644	0.2575
    */
  def H_x: Double = {
    entropyFromProbabilities(Vector(aa, bb))
  }

  /**
    * H(Y) 	= c*log(1/c)	 + d*log(1/d)
    * 0.8813	0.5211	0.3602
    */
  def H_y: Double = {
    entropyFromProbabilities(Vector(cc, dd))
  }

  /**
    * H(X,Y)	= e*log(1/e)	+ f*log(1/f)	+ g*Log(1/g)	+ h*log(1/h)
    * 1.5710	0.3322	0.3322	0.4644	0.4422
    */
  def H_xy: Double = {
    entropyFromProbabilities(Vector(ee, ff, gg, hh))
  }

  /**
    * Mutual Information I(X:Y) = Relative Entropy of Joint and Product Distributions ---
    * D(p(X,Y||p(X)p(Y)) = e*log(e/ac) 	0.073696559	+ f*log(f/ad)	-0.048542683	+ g*log(g/bc)	-0.052606881	+ h*log(h/bd)	0.059721404
    */
  def KL_divergence: Double = {
    ee * log(ee / (aa * cc), 2) + ff * log(ff/(aa*dd), 2) + gg * log(gg/(bb*cc), 2) + hh * log(hh/(bb*dd), 2)
  }

  /**
    * H(Y|X)	=                  (a	*H(e/a, f/a)) 	+	                   (b	*H(g/b, h/b)
    * 0.2000	1.0000		0.8000	0.8113
    */
  def H_y_given_x: Double = {
    aa * entropyFromProbabilities(Seq(true_P_rate, false_N_rate)) + bb * entropyFromProbabilities(Seq(false_P_rate, true_N_rate))
  }

  /**
    * H(X|Y)	=                  (c	*H(e/c, g/c)	+ 	                  (d	*H(f/d, h/d)
    * 0.3000	0.9183		0.7000	0.5917
    * 0.6897
    */
  def H_x_given_y: Double = {
    cc * entropyFromProbabilities(Seq(ee/cc, gg/cc)) + dd * entropyFromProbabilities(Seq(ff/dd, hh/dd))
  }

  /**
    * I(X;Y) =  	H(X) 	- H(X|Y)
    * 0.0323	0.7219	0.6897
    *
    * I(X;Y) = 	H(Y) 	 - H(Y|X)
    * 0.0323	0.8813	0.8490
    *
    * I(X;Y) = 	H(X) 	+ H(Y) 	 - H(X,Y)
    * 0.0323	0.7219	0.8813	1.5710
    */
  def I_xy: Double = {
    H_x - H_x_given_y
  }

  /**
    * P(Correctly classified)
    */
  val accuracy: Double = TP + TN

  /**
    * P(actual P| all P)
    */
  val precision: Double = TP / (TP + FP)

  /**
    * P(actual P| relevant P)
    * https://en.wikipedia.org/wiki/Precision_and_recall
    */
  val recall: Double = TP / (TP + FN)

  /**
    * harmonic mean
    * ranges between 0 (Worst) and 1 (Best)
    */
  val FMeasure: Double = (2 * precision * recall) / (precision + recall)

  /**
    * geometric mean
    */
  val GMeasure: Double = math.sqrt(precision * recall)



}

